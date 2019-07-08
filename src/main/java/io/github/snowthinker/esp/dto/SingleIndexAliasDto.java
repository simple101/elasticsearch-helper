package io.github.snowthinker.esp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SingleIndexAliasDto {

	private String index;
	
	private String alias;
}
