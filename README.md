# elasticsearch-helper ES助手

### 添加pom依赖
~~~xml
<dependency>
	<groupId>io.github.snowthinker</groupId>
	<artifactId>elasticsearch-helper</artifactId>
	<version>0.0.1-RELEASE</version>
</dependency>	
~~~

#### 初始化CustomElasticsearchTemplate
~~~java
@Bean
public CustomElasticsearchTemplate customElasticsearchTemplate(Client client) {
	return new CustomElasticsearchTemplate(client, new CustomDefaultResultMapper());
}
~~~


#### JavaBean编写
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

#### 深度分页
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

