package io.github.snowthinker.eh.page;

import org.springframework.data.domain.AbstractPageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

@SuppressWarnings("serial")
public class ElasticsearchPage extends AbstractPageRequest {
	
	private Order[] orders;

	public ElasticsearchPage(int page, int size) {
		super(page, size);
	}
	
	public ElasticsearchPage(int page, int size, Order... orders) {
		super(page, size);
		this.orders = orders;
	}

	@Override
	public Sort getSort() {
		if(null != orders || orders.length == 0) {
			return null;
		}
		return Sort.by(orders);
	}

	@Override
	public Pageable next() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Pageable previous() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Pageable first() {
		// TODO Auto-generated method stub
		return null;
	}

}
