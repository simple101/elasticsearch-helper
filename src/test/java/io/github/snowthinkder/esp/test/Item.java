package io.github.snowthinkder.esp.test;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.Data;

@SuppressWarnings("serial")
@Data	
public abstract class Item implements Serializable {

	@Transient
	@JsonIgnore
	private String actualIndex;
	
	@Id
	private String id;
	
	@Field
	private String name;
	
	@Field
	private String sku;
	
	@Field(type=FieldType.Date)
	@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss.SSS")
	@JsonSerialize(using=LocalDateTimeSerializer.class)
	@JsonDeserialize(using=LocalDateTimeDeserializer.class)
	private LocalDateTime createTime;
	
	@Field(type=FieldType.Date)
	@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss.SSS")
	@JsonSerialize(using=LocalDateTimeSerializer.class)
	@JsonDeserialize(using=LocalDateTimeDeserializer.class)
	private LocalDateTime updateTime;
}
