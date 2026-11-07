package desarrolloWeb.backend.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

/* Valida la foto que manda el celular al marcar: JPEG real, tamaño razonable y que no esté en negro
   (cámara tapada). La ubicación impresa la dibuja el celular; los datos que valen son los que calcula el servidor. */
public final class FotoMarcacion {

    public static final String TIPO_MIME = "image/jpeg";
    public static final int MAX_BYTES = 600 * 1024;
    public static final int LADO_MINIMO = 240;
    /* Brillo promedio (0 a 255) por debajo del cual la foto se considera tapada o a oscuras */
    static final double BRILLO_MINIMO = 18;

    private FotoMarcacion() {
    }

    /* Recibe el JPEG en Base64, con o sin el prefijo "data:image/jpeg;base64,". Devuelve los bytes validados. */
    public static byte[] validar(String base64) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalArgumentException("Falta la foto: tómate una foto para marcar");
        }
        String datos = base64.strip();
        int coma = datos.indexOf(',');
        if (datos.startsWith("data:") && coma > 0) {
            datos = datos.substring(coma + 1);
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(datos);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La foto no se pudo leer. Tómala de nuevo.");
        }
        if (bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("La foto pesa demasiado (máximo " + MAX_BYTES / 1024 + " KB)");
        }
        if (bytes.length < 3 || (bytes[0] & 0xFF) != 0xFF || (bytes[1] & 0xFF) != 0xD8 || (bytes[2] & 0xFF) != 0xFF) {
            throw new IllegalArgumentException("La foto debe ser una imagen JPEG tomada con la cámara");
        }

        BufferedImage imagen;
        try {
            imagen = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            imagen = null;
        }
        if (imagen == null) {
            throw new IllegalArgumentException("La foto está dañada. Tómala de nuevo.");
        }
        if (Math.min(imagen.getWidth(), imagen.getHeight()) < LADO_MINIMO) {
            throw new IllegalArgumentException("La foto es muy pequeña (mínimo " + LADO_MINIMO + " px por lado)");
        }
        if (brilloPromedio(imagen) < BRILLO_MINIMO) {
            throw new IllegalArgumentException("La foto salió muy oscura. Destapa la cámara y busca un lugar con luz.");
        }
        return bytes;
    }

    /* Muestrea una cuadrícula de la parte superior (la franja de abajo lleva el texto impreso sobre fondo oscuro) */
    static double brilloPromedio(BufferedImage imagen) {
        int ancho = imagen.getWidth();
        int alto = (int) (imagen.getHeight() * 0.7);
        int pasos = 24;
        double suma = 0;
        int muestras = 0;
        for (int i = 0; i < pasos; i++) {
            for (int j = 0; j < pasos; j++) {
                int x = (int) ((i + 0.5) * ancho / pasos);
                int y = (int) ((j + 0.5) * alto / pasos);
                int rgb = imagen.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                suma += 0.299 * r + 0.587 * g + 0.114 * b;
                muestras++;
            }
        }
        return suma / muestras;
    }
}
