package io.github.snowthinker.esp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RemoveAliasDto<T extends Object> {

	private T remove;
}
