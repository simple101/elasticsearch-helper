package io.github.snowthinker.eh;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;

import io.github.snowthinker.eh.dto.AddAliasDto;
import io.github.snowthinker.eh.dto.AliasActionDto;
import io.github.snowthinker.eh.dto.RemoveAliasDto;
import io.github.snowthinker.eh.dto.SingleIndexAliasDto;


public class EsIndicesMonthlyAutoCreator {
	
	DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyyMM");

	/**
	 * item, item
	 */
	private final Map<String, String> indexNames;
	
	private EsDataPartitioner esDataPartitioner;
	
	/**
	 * <p>连续月数据
	 */
	private Integer offset;
	
	public EsIndicesMonthlyAutoCreator(EsDataPartitioner esDataPartitioner, Map<String, String> indexNames, Integer offset) {
		this.indexNames = indexNames;
		this.esDataPartitioner = esDataPartitioner;
		this.offset = offset;
	}
	
	/**
	 * <p>创建当前月份offset个月的索引
	 * @param offset 必需为负值
	 */
	public void checkAndCreateIndex() {
		
		for(int i = offset; i<0; i++) {
			LocalDate date = LocalDate.now();
			LocalDate monthDate = date.plusMonths(i);
			String month = monthDate.format(monthFormatter);
			
			for(Iterator<String> iter = indexNames.keySet().iterator(); iter.hasNext();) {
				String indexName = iter.next();
				String typeName = indexNames.get(indexName);
				
				esDataPartitioner.createIndexByName(indexName, typeName, month);
			}
		}
	}
	
	/**
	 * <p>每天 23点50自动创建下个月的索引
	 */
	@Scheduled(cron="0 50 23 * * ?")
	public void autoCreateInices() {
		LocalDate date = LocalDate.now();
		LocalDate monthDate = date.plusMonths(1);
		String month = monthDate.format(monthFormatter);
		
		for(Iterator<String> iter = indexNames.keySet().iterator(); iter.hasNext();) {
			String indexName = iter.next();
			String typeName = indexNames.get(indexName);
			
			esDataPartitioner.createIndexByName(indexName, typeName, month);
		}
	}
	
	/**
	 * <p>每天23:59:55秒自动检查是不是本月最后一天，如果是则自动做别名切换
	 */
	@Scheduled(cron="55 59 23 * * ?")
	public void autoReAliasMonthy() {
		LocalDate today = LocalDate.now();
		LocalDate tomorrow = today.plusDays(1);
		
		if(tomorrow.getMonthValue() > today.getMonthValue()) {
			String currentMonth = today.format(monthFormatter);
			
			LocalDate nextMonthDate = today.plusMonths(1);
			String nextMonth = nextMonthDate.format(monthFormatter);
			
			// 切换写别名
			esDataPartitioner.reallocateWriteAlias(indexNames.keySet().toArray(new String[] {}), nextMonth, currentMonth);
			
			// 切换读别名
			AliasActionDto<Object> actionDto = this.buildReadAllocateAlias();
			esDataPartitioner.reallocateReadAlias(actionDto);
		}
	}

	private AliasActionDto<Object> buildReadAllocateAlias() {
		
		LocalDate today = LocalDate.now();
		LocalDate nextMonthDate = today.plusMonths(1);
		String nextMonth = nextMonthDate.format(monthFormatter);
		
		LocalDate removeMonthDate = today.plusMonths(offset);
		String removeMonth = removeMonthDate.format(monthFormatter);
		
		AliasActionDto<Object> actionDto = new AliasActionDto<>();
		
		for(Iterator<String> iter = indexNames.keySet().iterator(); iter.hasNext();) {
			String indexName = iter.next();
			
			// 删除别名
			SingleIndexAliasDto removeDto = new SingleIndexAliasDto(indexName + removeMonth, indexName + EsDataPartitioner.READ_INDEX_SUFFIX);
			actionDto.getActions().add(new RemoveAliasDto<>(removeDto));
			
			// 添加别名
			SingleIndexAliasDto addDto = new SingleIndexAliasDto(indexName + nextMonth, indexName + EsDataPartitioner.READ_INDEX_SUFFIX);
			actionDto.getActions().add(new AddAliasDto<>(addDto));
		}
		
		return actionDto;
	}
}
