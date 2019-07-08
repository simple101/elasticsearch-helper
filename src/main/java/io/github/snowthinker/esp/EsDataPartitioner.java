package io.github.snowthinker.esp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.snowthinker.esp.dto.AddAliasDto;
import io.github.snowthinker.esp.dto.AliasActionDto;
import io.github.snowthinker.esp.dto.MultipleIndiesAliasDto;
import io.github.snowthinker.esp.dto.RemoveAliasDto;
import io.github.snowthinker.esp.dto.SingleIndexAliasDto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EsDataPartitioner {
	
	public static final String READ_INDEX_SUFFIX = "_read";

	private String esHttpUrl;
	private RestTemplate restTemplate;
	
	public EsDataPartitioner(String esHttpUrl, RestTemplate restTemplate) {
		this.esHttpUrl = esHttpUrl;
		this.restTemplate = restTemplate;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean createIndexByName(String indexName, String typeName, String month) {
		boolean exist = checkIndexExist(indexName, typeName, month);
		
		if(exist) {
			return exist;
		}
		
		ObjectMapper om = new ObjectMapper();
		String path = String.format("elasticsearch/%s.json", typeName);
		String settings = ElasticsearchTemplate.readFileFromClasspath(path);
		
		String url = this.esHttpUrl + "/" + indexName + month;
		ResponseEntity<PutRepositoryResponse> responseEntity = null;
		
		try {
			HashMap<String, Object> params = om.readValue(settings, HashMap.class);
			HttpEntity<HashMap> requestEntity = new HttpEntity<>(params);
			responseEntity = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, PutRepositoryResponse.class);
		} catch (IOException e) {
			log.error("create partition error", e);
		}
		
		if(null == responseEntity || null == responseEntity.getStatusCode()) {
			log.error("create partition error: {}", responseEntity);
			return false;
		}
		
		AcknowledgedResponse responseData = responseEntity.getBody();
		return responseData.isAcknowledged();
	}
	

	/**
	 * <p>检查索引是否存在 
	 * @param indexName
	 * @param typeName
	 * @param month
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private boolean checkIndexExist(String indexName, String typeName, String month) {
		String fullIndexName = indexName + month;
		String url = this.esHttpUrl + "/" + fullIndexName;
		
		ResponseEntity<HashMap> responseEntity = null;
		
		try {
			responseEntity = restTemplate.getForEntity(url, HashMap.class);
		} catch (RestClientException e) {
			log.error("Check index error", e.getMessage());
		}
		
		if(null == responseEntity || null == responseEntity.getStatusCode() || null == responseEntity.getBody()) {
			return false;
		}
		
		Map<String, Object> rsMap = responseEntity.getBody();
		if(null != rsMap && null != rsMap.get(fullIndexName)) {
			return true;
		}
		return false;
	}
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public boolean reallocateWriteAlias(String[] indexNames, String bindMonth, String removeMonth) {
		String url = this.esHttpUrl + "/_aliases";
		ResponseEntity<HashMap> responseEntity = null;
		
		ObjectMapper om = new ObjectMapper();
		
		AliasActionDto actionDto = new AliasActionDto();
		for(String indexName : indexNames) {
			String removeAliasName = indexName + removeMonth;
			
			//删除别名
			SingleIndexAliasDto removeDto = new SingleIndexAliasDto(removeAliasName, indexName);
			actionDto.getActions().add(new RemoveAliasDto(removeDto));
			
			String addAliasName = indexName + bindMonth;
			
			//添加别名
			SingleIndexAliasDto addDto = new SingleIndexAliasDto(addAliasName, indexName);
			actionDto.getActions().add(new AddAliasDto(addDto));
		}
		
		try {
			String json = om.writeValueAsString(actionDto);
			
			log.info("Post url: {} data: \r\n{}", url, json);
			
			responseEntity = restTemplate.postForEntity(url, actionDto, HashMap.class);
		} catch (Exception e) {
			log.error("create alias error", e);
		} 
		
		if(null == responseEntity || null == responseEntity.getStatusCode() || null == responseEntity.getBody()) {
			return false;
		}
		
		Map<String, Object> rsMap = responseEntity.getBody();
		if(null != rsMap && null != rsMap.get("acknowledged")) {
			return true;
		}
		
		return false;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean reallocateReadAlias(AliasActionDto actionDto) {
		String url = this.esHttpUrl + "/_aliases";
		ResponseEntity<HashMap> responseEntity = null;
		
		ObjectMapper om = new ObjectMapper();
		
		try {
			String json = om.writeValueAsString(actionDto);
			
			log.info("Post url: {} data: \r\n{}", url, json);
			
			responseEntity = restTemplate.postForEntity(url, actionDto, HashMap.class);
		} catch (Exception e) {
			log.error("create alias error", e);
		} 
		
		if(null == responseEntity || null == responseEntity.getStatusCode() || null == responseEntity.getBody()) {
			return false;
		}
		
		Map<String, Object> rsMap = responseEntity.getBody();
		if(null != rsMap && null != rsMap.get("acknowledged")) {
			return true;
		}
		
		return false;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean createReadAlias(String[] indexNames, String[] bindMonths, String[] removeMonths) {
		String url = this.esHttpUrl + "/_aliases";
		ResponseEntity<HashMap> responseEntity = null;
		
		ObjectMapper om = new ObjectMapper();
		
		try {
			AliasActionDto<Object> actionDto = new AliasActionDto<>();
			
			for(String indexName : indexNames) {
				String[] removeIndices = new String[removeMonths.length];
				for (int i=0; i< removeMonths.length; i++) {
					String remoeAliasName = indexName + removeMonths[i];
					removeIndices[i] = remoeAliasName;
				}
				
				// 删除别名
				MultipleIndiesAliasDto removeDto = new MultipleIndiesAliasDto(removeIndices, indexName + READ_INDEX_SUFFIX);
				actionDto.getActions().add(new RemoveAliasDto<MultipleIndiesAliasDto>(removeDto));
				
				String[] bindIndices = new String[bindMonths.length];
				for(int i=0; i<bindMonths.length; i++) {
					String bindAliasName = indexName + bindMonths[i];
					bindIndices[i] = bindAliasName;
				}
				
				//添加别名
				MultipleIndiesAliasDto addDto = new MultipleIndiesAliasDto(bindIndices, indexName + READ_INDEX_SUFFIX);
				actionDto.getActions().add(new AddAliasDto<>(addDto));
			}
			
			String json = om.writeValueAsString(actionDto);
			
			log.info("Post url: {} data: \r\n{}", url, json);
			
			responseEntity = restTemplate.postForEntity(url, actionDto, HashMap.class);
		} catch (Exception e) {
			log.error("create alias error", e);
		} 
		
		if(null == responseEntity || null == responseEntity.getStatusCode() || null == responseEntity.getBody()) {
			return false;
		}
		
		Map<String, Object> rsMap = responseEntity.getBody();
		if(null != rsMap && null != rsMap.get("acknowledged")) {
			return true;
		}
		
		return false;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<String, Object> queryAliasByName(String indexName) {
		String url = this.esHttpUrl + "/_aliases/" + indexName;
		
		ResponseEntity<HashMap> responseEntity = null;
		
		try {
			responseEntity = restTemplate.getForEntity(url, HashMap.class);
		} catch (RestClientException e) {
			log.error("Check alias error", e.getMessage());
		}
		
		if(null == responseEntity || null == responseEntity.getStatusCode() || null == responseEntity.getBody()) {
			return null;
		}
		
		Map<String, Object> rsMap = responseEntity.getBody();
		if(null != rsMap) {
			return rsMap;
		}
		
		return null;
	}
	
	
}
