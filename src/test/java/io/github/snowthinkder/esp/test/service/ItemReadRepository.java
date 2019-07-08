package io.github.snowthinkder.esp.test.service;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import io.github.snowthinkder.esp.test.ItemRead;

@Repository
public interface ItemReadRepository extends ElasticsearchRepository<ItemRead, Long>{

}
