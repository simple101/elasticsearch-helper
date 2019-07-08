package io.github.snowthinker.esp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MultipleIndiesAliasDto {

	private String[] indices;
	
	private String alias;
}
