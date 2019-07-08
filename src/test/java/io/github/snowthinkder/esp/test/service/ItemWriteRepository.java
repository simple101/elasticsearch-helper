package io.github.snowthinkder.esp.test.service;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import io.github.snowthinkder.esp.test.ItemWrite;

@Repository
public interface ItemWriteRepository extends ElasticsearchRepository<ItemWrite, Long>{

}
