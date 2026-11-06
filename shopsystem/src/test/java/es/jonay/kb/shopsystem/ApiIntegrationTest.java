package es.jonay.kb.shopsystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestDatabase.class)
@WithMockUser(username = "admin@test", roles = "ADMIN")
class ApiIntegrationTest {

    private static final String CTX = "/kiosco";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    private long categoryId;

    private MockHttpServletRequestBuilder at(MockHttpServletRequestBuilder b) {
        return b.contextPath(CTX);
    }

    private String json(MockHttpServletRequestBuilder b, String body) throws Exception {
        return mvc.perform(at(b).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String t : new String[] { "stock_movements", "item_price_history", "trade_lines", "trades_items",
                "trades", "invoice_counters", "items", "categories" }) {
            jdbc.execute("DELETE FROM " + t);
        }
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 1");
        String cat = json(post(CTX + "/categories/"), "{\"name\":\"Bebidas\",\"igicRate\":7}");
        categoryId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(cat).get("id").asLong();
        json(post(CTX + "/items/"), """
                {"id":1001,"name":"Agua","price":1.20,"net":0.50,"categoryId":%d,"stock":10,"bareMinimun":3,"ignoreStock":false}
                """.formatted(categoryId));
    }

    @Test
    void precioConDecimalesExactosYHistorial() throws Exception {
        json(post(CTX + "/items/"), """
                {"id":1001,"name":"Agua","price":1.35,"net":0.50,"categoryId":%d,"stock":10,"originalStock":10,"bareMinimun":3}
                """.formatted(categoryId));

        mvc.perform(at(get(CTX + "/items/1001"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(1.35)).andExpect(jsonPath("$.net").value(0.50));
        mvc.perform(at(get(CTX + "/items/1001/price-history"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].price").value(1.35))
                .andExpect(jsonPath("$[0].changedBy").value("admin@test"))
                .andExpect(jsonPath("$[1].price").value(1.20));
    }

    @Test
    void movimientosDeStockPorApi() throws Exception {
        json(post(CTX + "/stock/movements"),
                "{\"type\":\"RESTOCK\",\"note\":\"Albarán 7\",\"lines\":[{\"itemId\":1001,\"quantity\":12,\"unitCost\":0.45}]}");
        json(post(CTX + "/stock/movements"),
                "{\"type\":\"LOSS\",\"note\":\"Rotura\",\"lines\":[{\"itemId\":1001,\"quantity\":1}]}");

        mvc.perform(at(get(CTX + "/items/1001/stock-movements"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("LOSS"))
                .andExpect(jsonPath("$.content[0].typeLabel").value("Merma"))
                .andExpect(jsonPath("$.content[0].stockAfter").value(21))
                .andExpect(jsonPath("$.content[1].note").value("Albarán 7"))
                .andExpect(jsonPath("$.content[0].createdBy").value("admin@test"));
        mvc.perform(at(get(CTX + "/stock/movements?type=RESTOCK"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(at(get(CTX + "/stock/movement-types"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7));
    }

    @Test
    void compatibilidadConLaPantallaDeStockActual() throws Exception {
        json(post(CTX + "/stock/"), "{\"1001\":5}");
        byte[] xlsx = mvc.perform(at(delete(CTX + "/stock/")).contentType(MediaType.APPLICATION_JSON).content("{\"1001\":2}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(wb.getSheetAt(0).getRow(1).getCell(1).getStringCellValue()).isEqualTo("Agua");
            assertThat(wb.getSheetAt(0).getRow(1).getCell(3).getNumericCellValue()).isEqualTo(2.0);
        }
        mvc.perform(at(get(CTX + "/items/1001"))).andExpect(jsonPath("$.stock").value(13));
    }

    @Test
    void erroresConCodigoYMensajeClaros() throws Exception {
        mvc.perform(at(post(CTX + "/stock/movements")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"SALE\",\"lines\":[{\"itemId\":1001,\"quantity\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("no se puede registrar a mano")));
        mvc.perform(at(post(CTX + "/stock/movements")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"RESTOCK\",\"lines\":[{\"itemId\":424242,\"quantity\":1}]}"))
                .andExpect(status().isNotFound());
        mvc.perform(at(delete(CTX + "/items/424242"))).andExpect(status().isNotFound());
        mvc.perform(at(delete(CTX + "/categories/" + categoryId))).andExpect(status().isConflict());
        mvc.perform(at(get(CTX + "/items/0/10?sortBy=password"))).andExpect(status().isBadRequest());
        mvc.perform(at(post(CTX + "/items/")).contentType(MediaType.APPLICATION_JSON).content("{roto"))
                .andExpect(status().isBadRequest());
        mvc.perform(at(post(CTX + "/items/")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1002,\"name\":\"X\",\"price\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("negativo")));
    }

    @Test
    void informesPorApi() throws Exception {
        json(post(CTX + "/trade/newTrade?print=false&paymentMethod=CARD"), "[{\"id\":1001},{\"id\":1001}]");

        mvc.perform(at(get(CTX + "/graphs/summary"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.current.revenue").value(2.40))
                .andExpect(jsonPath("$.current.trades").value(1))
                .andExpect(jsonPath("$.current.cardRevenue").value(2.40))
                .andExpect(jsonPath("$.current.profit").value(1.24)); // 2,24 - 1,00
        mvc.perform(at(get(CTX + "/graphs/sales-over-time?granularity=day"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(30));
        mvc.perform(at(get(CTX + "/graphs/top-items?metric=units"))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Agua")).andExpect(jsonPath("$[0].units").value(2));
        mvc.perform(at(get(CTX + "/graphs/by-category"))).andExpect(jsonPath("$[0].category").value("Bebidas"));
        mvc.perform(at(get(CTX + "/graphs/by-payment-method"))).andExpect(jsonPath("$[1].trades").value(1));
        mvc.perform(at(get(CTX + "/graphs/by-hour"))).andExpect(jsonPath("$.length()").value(24));
        mvc.perform(at(get(CTX + "/graphs/by-weekday"))).andExpect(jsonPath("$.length()").value(7));
        mvc.perform(at(get(CTX + "/graphs/inventory"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.units").value(8))
                .andExpect(jsonPath("$.valueAtCost").value(4.00));
        mvc.perform(at(get(CTX + "/graphs/summary?from=2025-02-10&to=2025-02-01"))).andExpect(status().isBadRequest());
    }

    @Test
    void informeMensualEnExcel() throws Exception {
        json(post(CTX + "/trade/newTrade?print=false"), "[{\"id\":1001}]");
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Atlantic/Canary"));

        byte[] xlsx = mvc.perform(at(get(CTX + "/generate-report?month=" + today.getMonthValue() + "&year=" + today.getYear())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            assertThat(wb.getSheetAt(0).getRow(0).getCell(1).getStringCellValue()).isEqualTo("BEBIDAS");
            int lastRow = wb.getSheetAt(0).getLastRowNum();
            assertThat(wb.getSheetAt(0).getRow(lastRow).getCell(2).getNumericCellValue()).isEqualTo(1.20);
            assertThat(wb.getSheetAt(1).getRow(wb.getSheetAt(1).getLastRowNum()).getCell(5).getNumericCellValue())
                    .isEqualTo(0.62); // beneficio: 1,12 - 0,50
        }
    }

    @Test
    void soloSeAdmitenImagenesReales() throws Exception {
        BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(img, "png", png);

        String name = mvc.perform(at(multipart(CTX + "/images/").file(
                new MockMultipartFile("file", "foto.jpg", "image/jpeg", png.toByteArray()))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(name).startsWith("foto_").endsWith(".png"); // la extensión sale del contenido real

        mvc.perform(at(multipart(CTX + "/images/").file(
                new MockMultipartFile("file", "virus.png", "image/png", "<?php echo 1; ?>".getBytes()))))
                .andExpect(status().isBadRequest());
        mvc.perform(at(multipart(CTX + "/images/").file(new MockMultipartFile("file", "x.svg", "image/svg+xml",
                "<svg xmlns='http://www.w3.org/2000/svg'><script>alert(1)</script></svg>".getBytes()))))
                .andExpect(status().isBadRequest());

        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get("uploads", name));
    }
}
