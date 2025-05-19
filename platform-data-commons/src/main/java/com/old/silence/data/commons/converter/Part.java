package com.old.silence.data.commons.converter;

/**
 * @author MurrayZhang
 */
public class Part {

    public enum Type {
        EQUAL, NOT_EQUAL, GREATER_THAN, GREATER_THAN_EQUAL, LESS_THAN, LESS_THAN_EQUAL,
        IN, NOT_IN, LIKE, NOT_LIKE, STARTING_WITH, ENDING_WITH, CONTAINING, NOT_CONTAINING,
        IS_NULL, IS_NOT_NULL, SIMPLE_PROPERTY, NEGATING_SIMPLE_PROPERTY, BETWEEN, TRUE, FALSE
    }

    public enum Logic {
        AND, OR
    }
}