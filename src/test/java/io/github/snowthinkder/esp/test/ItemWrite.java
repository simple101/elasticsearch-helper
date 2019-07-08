package io.github.snowthinkder.esp.test;

import org.springframework.data.elasticsearch.annotations.Document;

@SuppressWarnings("serial")
@Document(indexName = "session_write", type="item")
public class ItemWrite extends Item {

}
