package io.github.snowthinkder.esp.test;

import org.springframework.data.elasticsearch.annotations.Document;

@SuppressWarnings("serial")
@Document(indexName = "item_write", type="item")
public class ItemWrite extends Item {

}
