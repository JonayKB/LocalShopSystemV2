package es.jonay.kb.shopsystem.model.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ItemTest {

    private static Item item(long id) {
        Item i = new Item();
        // new Long(...) evita la caché de Long (-128..127): son objetos distintos con el mismo valor.
        i.setId(Long.valueOf(String.valueOf(id)));
        return i;
    }

    @Test
    void mismosIdsSonIgualesTambienPorEncimaDe127() {
        assertThat(item(5)).isEqualTo(item(5));
        assertThat(item(128)).isEqualTo(item(128));
        assertThat(item(123456)).isEqualTo(item(123456)).hasSameHashCodeAs(item(123456));
    }

    @Test
    void idsDistintosONulosNoSonIguales() {
        assertThat(item(128)).isNotEqualTo(item(129));
        assertThat(new Item()).isNotEqualTo(item(1));
        assertThat(item(1)).isNotEqualTo(null).isNotEqualTo("1");
    }

    @Test
    void lasColeccionesEncuentranElProducto() {
        List<Item> list = new ArrayList<>(List.of(item(1000), item(2000)));

        assertThat(list).contains(item(2000));
        assertThat(list.remove(item(1000))).isTrue();
        assertThat(list).hasSize(1);
    }
}
