package org.meveo.commons.persistence.postgresql;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;

public class PostgreSQLStringAggDistinctFunction implements SQLFunction {

	@Override
	public String render(Type firstArgumentType, @SuppressWarnings("rawtypes") List args,
			SessionFactoryImplementor factory) throws QueryException {

		if (args.size() < 1) {
			throw new IllegalArgumentException("The function string_agg_long requires at least 1 argument");
		}
		String fieldName = (String) args.get(0);
		String separator = ",";
		if (args.size() > 1) {
			separator = (String) args.get(1);
		}

		return "string_agg(distinct concat(" + fieldName + ", ''), '" + separator + "')";

	}

	@Override
	public Type getReturnType(Type firstArgumentType, Mapping mapping) throws QueryException {
		return StringType.INSTANCE;
	}

	@Override
	public boolean hasArguments() {
		return true;
	}

	@Override
	public boolean hasParenthesesIfNoArguments() {
		return false;
	}
}