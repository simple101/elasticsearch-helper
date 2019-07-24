package io.github.snowthinkder.esp.test.service;

import java.time.format.DateTimeFormatter;

import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;

import io.github.snowthinkder.esp.test.ItemWrite;
import io.github.snowthinker.eh.template.CustomElasticsearchTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ItemWriteService {
	
	private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
	
	@Autowired
	private CustomElasticsearchTemplate customElasticsearchTemplate;
	
	public void update(ItemWrite itemWrite) {
		UpdateQuery query = new UpdateQuery();
		query.setClazz(ItemWrite.class);
		query.setId(itemWrite.getId());
		query.setIndexName(itemWrite.getActualIndex());
		query.setType("item");
		
		UpdateRequest updateRequest = new UpdateRequest();
		try {
			updateRequest.index(itemWrite.getActualIndex())
						.type("item")
						.id(itemWrite.getId())
						.doc(XContentFactory.jsonBuilder()
								.startObject()
								.field("name", itemWrite.getName())
								.field("updateTime", itemWrite.getUpdateTime().format(DATETIME_FORMATTER))
								.endObject()
								);
			
			query.setUpdateRequest(updateRequest);
			customElasticsearchTemplate.update(query);
		} catch (Exception e) {
			log.error("update failed", e);
		}
	}
}
