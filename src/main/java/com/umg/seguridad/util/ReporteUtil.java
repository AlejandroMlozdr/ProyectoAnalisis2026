package com.umg.seguridad.util;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.function.Supplier;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Genera archivos PDF y Excel a partir de una tabla generica (titulo +
 * columnas + filas de texto). Los controladores de cada pantalla solo arman
 * esos datos a partir de su propia lista de entidades y llaman a este
 * utilitario; asi no se repite la logica de reporteria (PDF/Excel) en cada
 * uno de los 9 mantenimientos.
 *
 * Uso tipico en un controlador:
 *   List<String> columnas = List.of("Nombre", "NIT", "Dirección");
 *   List<List<String>> filas = empresas.stream()
 *       .map(e -> ReporteUtil.fila(e.getNombre(), e.getNit(), e.getDireccion()))
 *       .toList();
 *   byte[] pdf = ReporteUtil.generarPdf("Listado de Empresas", columnas, filas);
 */
public final class ReporteUtil {

    private ReporteUtil() {
    }

    /** Arma una fila permitiendo valores null (List.of(...) los rechaza; Arrays.asList no). */
    public static List<String> fila(String... valores) {
        return Arrays.asList(valores);
    }

    /** Texto de cualquier valor para tablas: null -> "", fechas con formato dd/MM/yyyy. */
    public static String str(Object valor) {
        if (valor == null) return "";
        if (valor instanceof LocalDateTime dt) return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        if (valor instanceof LocalDate d) return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return valor.toString();
    }

    /** Monto con separador de miles y 2 decimales (ej. 11,000.00). */
    public static String money(BigDecimal valor) {
        if (valor == null) return "";
        return String.format(Locale.US, "%,.2f", valor);
    }

    /** Evalua una cadena de getters tolerando nulls intermedios (ej. e.getPuesto().getNombre()). */
    public static String safe(Supplier<Object> supplier) {
        try {
            return str(supplier.get());
        } catch (NullPointerException ex) {
            return "";
        }
    }

    public static byte[] generarPdf(String titulo, List<String> columnas, List<List<String>> filas) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(columnas.size() > 5 ? PageSize.A4.rotate() : PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph parrafoTitulo = new Paragraph(titulo, fontTitulo);
            parrafoTitulo.setAlignment(Element.ALIGN_CENTER);
            parrafoTitulo.setSpacingAfter(6f);
            document.add(parrafoTitulo);

            Font fontFecha = FontFactory.getFont(FontFactory.HELVETICA, 9);
            String fecha = "Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            Paragraph parrafoFecha = new Paragraph(fecha, fontFecha);
            parrafoFecha.setAlignment(Element.ALIGN_CENTER);
            parrafoFecha.setSpacingAfter(14f);
            document.add(parrafoFecha);

            PdfPTable tabla = new PdfPTable(columnas.size());
            tabla.setWidthPercentage(100);

            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            for (String col : columnas) {
                PdfPCell celda = new PdfPCell(new Phrase(col, fontHeader));
                celda.setHorizontalAlignment(Element.ALIGN_CENTER);
                celda.setPadding(6f);
                tabla.addCell(celda);
            }

            Font fontCelda = FontFactory.getFont(FontFactory.HELVETICA, 9);
            for (List<String> fila : filas) {
                for (String valor : fila) {
                    PdfPCell celda = new PdfPCell(new Phrase(valor == null ? "" : valor, fontCelda));
                    celda.setPadding(5f);
                    tabla.addCell(celda);
                }
            }
            if (filas.isEmpty()) {
                PdfPCell vacio = new PdfPCell(new Phrase("No hay registros para mostrar.", fontCelda));
                vacio.setColspan(columnas.size());
                vacio.setHorizontalAlignment(Element.ALIGN_CENTER);
                vacio.setPadding(10f);
                tabla.addCell(vacio);
            }

            document.add(tabla);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error al generar el PDF: " + e.getMessage(), e);
        }
    }

    public static byte[] generarExcel(String titulo, List<String> columnas, List<List<String>> filas) {
        String nombreHoja = titulo.length() > 31 ? titulo.substring(0, 31) : titulo;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = workbook.createSheet(nombreHoja);

            CellStyle estiloHeader = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font fontHeader = workbook.createFont();
            fontHeader.setBold(true);
            fontHeader.setColor(IndexedColors.WHITE.getIndex());
            estiloHeader.setFont(fontHeader);
            estiloHeader.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
            estiloHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row filaHeader = hoja.createRow(0);
            for (int i = 0; i < columnas.size(); i++) {
                Cell celda = filaHeader.createCell(i);
                celda.setCellValue(columnas.get(i));
                celda.setCellStyle(estiloHeader);
            }

            int numeroFila = 1;
            for (List<String> fila : filas) {
                Row filaExcel = hoja.createRow(numeroFila++);
                for (int i = 0; i < fila.size(); i++) {
                    filaExcel.createCell(i).setCellValue(fila.get(i) == null ? "" : fila.get(i));
                }
            }

            for (int i = 0; i < columnas.size(); i++) {
                hoja.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error al generar el Excel: " + e.getMessage(), e);
        }
    }
}
