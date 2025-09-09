package es.jonay.kb.shopsystem.api.dto;

public record GraphDto<T>(
        String labels,
        T data) {
}
