package io.github.snowthinker.eh.page;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Order;

public class ElasticsearchPageHelper {

	
	public static Pageable pageable(Integer pageSize, Integer currentPage, Order... order) {
		return new ElasticsearchPage(currentPage, pageSize, order);
	}
}
