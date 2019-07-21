# elasticsearch-helper ES助手

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
    <version>0.0.1-RELEASE</version>
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
int currentPage = 1;

// 深度分页
Object[] searchAfters = new Object[]{"Iphone", "_id"};

// 排序必需和深度分页顺序相一致
Order[] orders = new Order[2];
orders[0] = Sort.Order.asc("name");
orders[1] = Sort.Order.asc("_id");

// 条件查询
QueryBuilder queryBuilder = QueryBuilders.termQuery("name", "Iphone");

Pageable pageable = ElasticsearchPageHelper.pageable(pageSize, currentPage, orders)
Page<Item> page = customElasticsearchTemplate.searchDeep(queryBuilder, pageable, Item.class, searchAfters);

List<Item> dataList = page.get().collect(Collectors.toList());
System.out.println(dataList);
~~~

### 5、打印DSL日志

~~~xml
<logger name="org.springframework.data.elasticsearch.core.QUERY" level="DEBUG"/>
~~~
