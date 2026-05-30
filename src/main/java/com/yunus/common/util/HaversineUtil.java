package com.yunus.common.util;

// Sınıf adı: HaversineUtil
// Amacı: İki coğrafi koordinat arasındaki yüzey mesafesini kilometre cinsinden hesaplar.
// Ne yapıyor: Haversine formülünü kullanarak dünya küresel yüzeyi üzerindeki
//             büyük daire mesafesini döner. İşletme yakınlık sorgularında
//             (lat/lon bazlı filtreleme) yardımcı utility olarak kullanılır.

/**
 * Haversine formülüne dayalı coğrafi mesafe hesaplama utility sınıfı.
 *
 * <p>Tüm metodlar statiktir; instantiate edilmesi engellenir.
 * Dünya yarıçapı 6371 km (ortalama küresel yarıçap) olarak sabitlenmiştir.
 */
public final class HaversineUtil {

    /** Dünya'nın ortalama yarıçapı (km). */
    private static final double EARTH_RADIUS_KM = 6371.0;

    // Utility sınıfı — dışarıdan instantiate edilemez
    private HaversineUtil() {
        throw new UnsupportedOperationException("HaversineUtil utility sınıfıdır, instantiate edilemez.");
    }

    /**
     * İki coğrafi nokta arasındaki mesafeyi kilometre cinsinden hesaplar.
     *
     * <p>Haversine formülü:
     * <pre>
     *   dLat = toRadians(lat2 - lat1)
     *   dLon = toRadians(lon2 - lon1)
     *   a    = sin(dLat/2)² + cos(toRadians(lat1)) * cos(toRadians(lat2)) * sin(dLon/2)²
     *   c    = 2 * atan2(√a, √(1−a))
     *   d    = 6371 * c
     * </pre>
     *
     * @param lat1 birinci noktanın enlemi (derece)
     * @param lon1 birinci noktanın boylamı (derece)
     * @param lat2 ikinci noktanın enlemi (derece)
     * @param lon2 ikinci noktanın boylamı (derece)
     * @return iki nokta arasındaki büyük daire mesafesi (km)
     */
    public static double calculate(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
