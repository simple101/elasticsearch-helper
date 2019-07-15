package io.github.snowthinker.eh.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MultipleIndiesAliasDto {

	private String[] indices;
	
	private String alias;
}
