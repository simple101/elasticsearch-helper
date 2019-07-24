package io.github.snowthinkder.esp.test.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;

import io.github.snowthinkder.esp.test.Item;
import io.github.snowthinker.eh.template.CustomElasticsearchTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ItemReadService {

	@Autowired
	private CustomElasticsearchTemplate customElasticsearchTemplate;
	
	@SuppressWarnings("rawtypes")
	public Page<Item> searchAfter() {
		int pageSize = 10;

		// 深度分页
		Object[] searchAfters = new Object[]{"Iphone", "_id"};

		// 排序必需和深度分页顺序相一致
		SortBuilder[] orders = new SortBuilder[2];
		orders[0] = SortBuilders.fieldSort("name.keyword").order(SortOrder.DESC);
		orders[1] = SortBuilders.fieldSort("id.keyword").order(SortOrder.DESC);

		// 条件查询
		QueryBuilder queryBuilder = QueryBuilders.termQuery("name", "Iphone");
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withIndices("item")
				.withQuery(queryBuilder)
				.build();
		
		Page<Item> pageResult = customElasticsearchTemplate.searchAfter(searchQuery, Arrays.asList(orders), searchAfters, pageSize, Item.class);
		
		List<Item> dataList = pageResult.get().collect(Collectors.toList());
		
		log.info("Total Record: {}, page list: {}", pageResult.getTotalElements(), dataList);
		
		return pageResult;
	}
}
