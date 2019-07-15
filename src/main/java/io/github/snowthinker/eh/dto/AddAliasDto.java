package io.github.snowthinker.eh.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddAliasDto<T extends Object> {

	private T add;
}
