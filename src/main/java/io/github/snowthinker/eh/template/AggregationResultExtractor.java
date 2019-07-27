package io.github.snowthinker.eh.template;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.metrics.cardinality.InternalCardinality;
import org.elasticsearch.search.aggregations.metrics.sum.InternalSum;
import org.elasticsearch.search.aggregations.metrics.valuecount.InternalValueCount;

import io.github.snowthinker.model.PojoHelper;

public class AggregationResultExtractor {

	public static Map<String, Object> extractResult(List<Aggregation> aggList) {
		if(null == aggList || aggList.isEmpty()) {
			return null;
		}
		
		Map<String, Object> rsMap = new HashMap<>();
		
		aggList.forEach(agg -> {
			String name = agg.getName();
			
			if(agg instanceof InternalSum) {
				InternalSum sum = (InternalSum) agg;
				rsMap.put(name, sum.getValue());
			} else if(agg instanceof InternalValueCount) {
				InternalValueCount count = (InternalValueCount) agg;
				rsMap.put(name, count.getValue());
			} else if(agg instanceof InternalCardinality) {
				InternalCardinality cardinality = (InternalCardinality) agg;
				rsMap.put(name, cardinality.getValue());
			}
		});
		return rsMap;
	}
	
	public <T extends Object> T extractResult(List<Aggregation> aggList, Class<T> clazz) {
		Map<String, Object> rsMap = extractResult(aggList);
		return PojoHelper.convertMap2Pojo(rsMap, clazz);
	}
}
