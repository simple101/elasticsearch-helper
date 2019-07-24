# elasticsearch-helper ES助手(https://github.com/snowThinker/elasticsearch-helper)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.snowthinker/elasticsearch-helper.svg?label=Maven%20Central)](https://mvnrepository.com/artifact/io.github.snowthinker/elasticsearch-helper)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

## 主要功能

* **ElasticsearchTemplate深度分页支持**：重写ElasticsearchTemplate支持 search after 深度分页
* **自动创建分区及自动分区指向**：可按月分区索引，每月创建索引，自动别名指向功能
* **Elasticsearch 读写分离**：读写分离, 读操作所有索引分区，写只操作当月索引

## 如何使用

### 1、添加pom依赖

~~~xml
<dependency>
    <groupId>io.github.snowthinker</groupId>
    <artifactId>elasticsearch-helper</artifactId>
    <version>0.0.2-RELEASE</version>
</dependency>
~~~

### 2、初始化CustomElasticsearchTemplate

~~~java
@Bean
public CustomElasticsearchTemplate customElasticsearchTemplate(Client client) {
	return new CustomElasticsearchTemplate(client, new CustomDefaultResultMapper());
}
~~~

### 3、JavaBean编写

~~~java
@Data
public class Item implements Serializable {	
	@Id
	private String id;
	
	@Field
	private String name;
	
	@Field
	private String sku;
}
~~~

### 4、深度分页

~~~java
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
~~~

### 5、打印DSL日志

~~~xml
<logger name="org.springframework.data.elasticsearch.core.QUERY" level="DEBUG"/>
~~~
