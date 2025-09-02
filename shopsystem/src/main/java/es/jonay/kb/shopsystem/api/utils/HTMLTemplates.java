package es.jonay.kb.shopsystem.api.utils;

import java.util.List;

public class HTMLTemplates {
    private HTMLTemplates() {
    }

    private static final String COMMON_STYLES = """
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        margin: 0;
                        padding: 0;
                        background-color: #f4f4f9;
                        color: #333;
                    }
                    .container {
                        max-width: 600px;
                        margin: 50px auto;
                        padding: 20px;
                        background: #ffffff;
                        border-radius: 10px;
                        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                    }
                    .header {
                        background: rgb(130, 76, 175);
                        color: #ffffff;
                        padding: 10px;
                        border-radius: 10px 10px 0 0;
                        text-align: center;
                    }
                    .content {
                        padding: 20px;
                        text-align: center;
                    }
                    .button {
                        display: inline-block;
                        margin-top: 20px;
                        padding: 10px 20px;
                        font-size: 16px;
                        color: #ffffff;
                        background-color: rgb(175, 76, 167);
                        text-decoration: none;
                        border-radius: 5px;
                    }
                    .button:hover {
                        background-color: rgb(139, 69, 160);
                    }
                    .footer {
                        margin-top: 20px;
                        font-size: 12px;
                        color: #999;
                        text-align: center;
                    }
                </style>
            """;

    public static final String ERROR = """
            <html>
            <head><title>Error</title>%s</head>
            <body>
                <div class='container'>
                    <div class='header'>
                        <h1>Error al verificar</h1>
                    </div>
                    <div class='content'>
                        <p>Ha ocurrido un error al verificar tu cuenta.</p>
                        <p><strong>%s</strong></p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(COMMON_STYLES, "%s");

    public static final String BAD_REQUEST = """
            <html>
            <head><title>Error</title>%s</head>
            <body>
                <div class='container'>
                    <div class='header'>
                        <h1>Error al verificar</h1>
                    </div>
                    <div class='content'>
                        <p>Ha ocurrido un error al verificar tu cuenta.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(COMMON_STYLES);

    public static String lowStockAlert(List<String> productNames) {
        StringBuilder productListHtml = new StringBuilder("<ul style='text-align:left;'>");
        for (String product : productNames) {
            productListHtml.append("<li>").append(product).append("</li>");
        }
        productListHtml.append("</ul>");

        return """
                <html>
                <head><title>Alerta de Stock</title>%s</head>
                <body>
                    <div class='container'>
                        <div class='header'>
                            <h1>Productos sin stock</h1>
                        </div>
                        <div class='content'>
                            <p>Los siguientes productos están sin stock:</p>
                            %s
                        </div>
                        <div class='footer'>
                            Este es un mensaje automático del sistema de stock.
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(COMMON_STYLES, productListHtml.toString());
    }

}
