package io.github.snowthinker.eh.template;

import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.List;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.DefaultResultMapper;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.EntityMapper;
import org.springframework.data.elasticsearch.core.ResultsExtractor;
import org.springframework.data.elasticsearch.core.ResultsMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.facet.FacetRequest;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.IndexBoost;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.ScriptField;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.core.query.SourceFilter;
import org.springframework.util.Assert;

@SuppressWarnings("deprecation")
public class CustomElasticsearchTemplate extends ElasticsearchTemplate {
	
	private static final Logger QUERY_LOGGER = LoggerFactory.getLogger("org.springframework.data.elasticsearch.core.QUERY");
	
	private static final String FIELD_SCORE = "_score";
	
	private Client client;
	
	@SuppressWarnings("unused")
	private ElasticsearchConverter elasticsearchConverter;
	
	private ResultsMapper resultsMapper;
	
	private String searchTimeout;

	
	public CustomElasticsearchTemplate(Client client) {
		this(client, new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()));
	}
	
	public CustomElasticsearchTemplate(Client client, EntityMapper entityMapper) {
		this(client, new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()), entityMapper);
	}

	public CustomElasticsearchTemplate(Client client, ElasticsearchConverter elasticsearchConverter,
			EntityMapper entityMapper) {
		this(client, elasticsearchConverter,
				new DefaultResultMapper(elasticsearchConverter.getMappingContext(), entityMapper));
	}

	public CustomElasticsearchTemplate(Client client, ResultsMapper resultsMapper) {
		this(client, new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()), resultsMapper);
	}

	public CustomElasticsearchTemplate(Client client, ElasticsearchConverter elasticsearchConverter) {
		this(client, elasticsearchConverter, new DefaultResultMapper(elasticsearchConverter.getMappingContext()));
	}

	public CustomElasticsearchTemplate(Client client, ElasticsearchConverter elasticsearchConverter,
			ResultsMapper resultsMapper) {
		
		super(client, elasticsearchConverter, resultsMapper);
		
		Assert.notNull(client, "Client must not be null!");
		Assert.notNull(elasticsearchConverter, "ElasticsearchConverter must not be null!");
		Assert.notNull(resultsMapper, "ResultsMapper must not be null!");

		this.client = client;
		this.elasticsearchConverter = elasticsearchConverter;
		this.resultsMapper = resultsMapper;
		
	}
	
	public <T> AggregatedPage<T> searchDeep(QueryBuilder query, Pageable pageable, Class<T> clazz, Object... afterSearch) {
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(query).withPageable(pageable).build();
		SearchRequestBuilder requestBuilder = prepareSearch(searchQuery, clazz);
		
		// deep search, add after_search conditions
		if(null != afterSearch && afterSearch.length > 0) {
			requestBuilder.searchAfter(afterSearch);	
		}
		
		SearchResponse response = doSearch(requestBuilder, searchQuery);
		return resultsMapper.mapResults(response, clazz, pageable);
	}
	
	@SuppressWarnings("rawtypes")
	private String[] retrieveIndexNameFromPersistentEntity(Class clazz) {
		if (clazz != null) {
			return new String[] { getPersistentEntityFor(clazz).getIndexName() };
		}
		return null;
	}
	
	@SuppressWarnings("rawtypes")
	private String[] retrieveTypeFromPersistentEntity(Class clazz) {
		if (clazz != null) {
			return new String[] { getPersistentEntityFor(clazz).getIndexType() };
		}
		return null;
	}
	
	@SuppressWarnings("rawtypes")
	private void setPersistentEntityIndexAndType(Query query, Class clazz) {
		if (query.getIndices().isEmpty()) {
			query.addIndices(retrieveIndexNameFromPersistentEntity(clazz));
		}
		if (query.getTypes().isEmpty()) {
			query.addTypes(retrieveTypeFromPersistentEntity(clazz));
		}
	}
	
	private static String[] toArray(List<String> values) {
		String[] valuesAsArray = new String[values.size()];
		return values.toArray(valuesAsArray);
	}
	
	private SearchRequestBuilder prepareSearch(Query query) {
		Assert.notNull(query.getIndices(), "No index defined for Query");
		Assert.notNull(query.getTypes(), "No type defined for Query");

		int startRecord = 0;
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(toArray(query.getIndices()))
				.setSearchType(query.getSearchType())
				.setTypes(toArray(query.getTypes()))
				.setVersion(true)
				.setTrackScores(query.getTrackScores());

		if (query.getSourceFilter() != null) {
			SourceFilter sourceFilter = query.getSourceFilter();
			searchRequestBuilder.setFetchSource(sourceFilter.getIncludes(), sourceFilter.getExcludes());
		}

		if (query.getPageable().isPaged()) {
			startRecord = query.getPageable().getPageNumber() * query.getPageable().getPageSize();
			searchRequestBuilder.setSize(query.getPageable().getPageSize());
		}
		searchRequestBuilder.setFrom(startRecord);

		if (!query.getFields().isEmpty()) {
			searchRequestBuilder.setFetchSource(toArray(query.getFields()), null);
		}

		if (query.getIndicesOptions() != null) {
			searchRequestBuilder.setIndicesOptions(query.getIndicesOptions());
		}

		if (query.getSort() != null) {
			for (Sort.Order order : query.getSort()) {
				SortOrder sortOrder = order.getDirection().isDescending() ? SortOrder.DESC : SortOrder.ASC;

				if (FIELD_SCORE.equals(order.getProperty())) {
					ScoreSortBuilder sort = SortBuilders //
							.scoreSort() //
							.order(sortOrder);

					searchRequestBuilder.addSort(sort);
				} else {
					FieldSortBuilder sort = SortBuilders //
							.fieldSort(order.getProperty()) //
							.order(sortOrder);

					if (order.getNullHandling() == Sort.NullHandling.NULLS_FIRST) {
						sort.missing("_first");
					} else if (order.getNullHandling() == Sort.NullHandling.NULLS_LAST) {
						sort.missing("_last");
					}

					searchRequestBuilder.addSort(sort);
				}
			}
		}

		if (query.getMinScore() > 0) {
			searchRequestBuilder.setMinScore(query.getMinScore());
		}
		return searchRequestBuilder;
	}
	
	private <T> SearchRequestBuilder prepareSearch(Query query, Class<T> clazz) {
		setPersistentEntityIndexAndType(query, clazz);
		return prepareSearch(query);
	}
	
	@SuppressWarnings({ "rawtypes" })
	private SearchResponse doSearch(SearchRequestBuilder searchRequest, SearchQuery searchQuery) {
		
		if (searchQuery.getFilter() != null) {
			searchRequest.setPostFilter(searchQuery.getFilter());
		}

		if (!isEmpty(searchQuery.getElasticsearchSorts())) {
			for (SortBuilder sort : searchQuery.getElasticsearchSorts()) {
				searchRequest.addSort(sort);
			}
		}

		if (!searchQuery.getScriptFields().isEmpty()) {
			// _source should be return all the time
			// searchRequest.addStoredField("_source");
			for (ScriptField scriptedField : searchQuery.getScriptFields()) {
				searchRequest.addScriptField(scriptedField.fieldName(), scriptedField.script());
			}
		}

		if (searchQuery.getHighlightFields() != null || searchQuery.getHighlightBuilder() != null) {
			HighlightBuilder highlightBuilder = searchQuery.getHighlightBuilder();
			if (highlightBuilder == null) {
				highlightBuilder = new HighlightBuilder();
			}
			for (HighlightBuilder.Field highlightField : searchQuery.getHighlightFields()) {
				highlightBuilder.field(highlightField);
			}
			searchRequest.highlighter(highlightBuilder);
		}

		if (!isEmpty(searchQuery.getIndicesBoost())) {
			for (IndexBoost indexBoost : searchQuery.getIndicesBoost()) {
				searchRequest.addIndexBoost(indexBoost.getIndexName(), indexBoost.getBoost());
			}
		}

		if (!isEmpty(searchQuery.getAggregations())) {
			for (AbstractAggregationBuilder aggregationBuilder : searchQuery.getAggregations()) {
				searchRequest.addAggregation(aggregationBuilder);
			}
		}

		if (!isEmpty(searchQuery.getFacets())) {
			for (FacetRequest aggregatedFacet : searchQuery.getFacets()) {
				searchRequest.addAggregation(aggregatedFacet.getFacet());
			}
		}
		
		return getSearchResponse(searchRequest.setQuery(searchQuery.getQuery()));
	}

	private SearchResponse getSearchResponse(SearchRequestBuilder requestBuilder) {

		if (QUERY_LOGGER.isDebugEnabled()) {
			QUERY_LOGGER.debug(requestBuilder.toString());
		}

		return getSearchResponse(requestBuilder.execute());
	}
	
	private SearchResponse getSearchResponse(ActionFuture<SearchResponse> response) {
		return searchTimeout == null ? response.actionGet() : response.actionGet(searchTimeout);
	}

	/**
	 * search after aggregation
	 * @param query
	 * @param searchAfters
	 * @param resultsExtractor
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public <T> T searchAfterAggregation(SearchQuery query, List<SortBuilder> sorts, Object[] searchAfters, 
			Integer pageSize, ResultsExtractor<T> resultsExtractor) {
		SearchRequestBuilder requestBuilder = prepareSearch(query);
		
		if(null != searchAfters && searchAfters.length > 0) {
			requestBuilder.searchAfter(searchAfters);	
		}
		
		if(null != sorts && sorts.size() > 0) {
			sorts.forEach(sort -> {
				requestBuilder.addSort(sort);
			});
		}
		
		requestBuilder.setSize(pageSize);
		
		SearchResponse response = doSearch(requestBuilder, query);
		return resultsExtractor.extract(response);
	}
}
