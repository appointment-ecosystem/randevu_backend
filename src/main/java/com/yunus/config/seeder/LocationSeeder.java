package com.yunus.config.seeder;

import com.yunus.location.entity.City;
import com.yunus.location.entity.District;
import com.yunus.location.entity.Neighborhood;
import com.yunus.location.repository.CityRepository;
import com.yunus.location.repository.DistrictRepository;
import com.yunus.location.repository.NeighborhoodRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Türkiye illeri, ilçeleri ve mahalleleri için başlangıç verisi.
 * Kayıt zaten varsa tekrar eklemez.
 */
@Component
public class LocationSeeder {

    private static final Logger log = LoggerFactory.getLogger(LocationSeeder.class);

    private final CityRepository cityRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;

    public LocationSeeder(CityRepository cityRepository,
                          DistrictRepository districtRepository,
                          NeighborhoodRepository neighborhoodRepository) {
        this.cityRepository = cityRepository;
        this.districtRepository = districtRepository;
        this.neighborhoodRepository = neighborhoodRepository;
    }

    @Transactional
    public void seed() {
        if (cityRepository.count() > 0) {
            log.debug("Location data already exists, skipping seed.");
            return;
        }

        seedCity("İzmir", "35",
                new String[][]{
                        {"Konak", "Alsancak", "Göztepe"},
                        {"Karşıyaka", "Bostanlı", "Mavişehir"},
                        {"Bornova", "Erzene", "Kazımdirik"}
                });
        seedCity("İstanbul", "34",
                new String[][]{
                        {"Kadıköy", "Moda", "Fenerbahçe"},
                        {"Beşiktaş", "Levent", "Ortaköy"},
                        {"Üsküdar", "Kuzguncuk", "Çengelköy"}
                });
        seedCity("Ankara", "6",
                new String[][]{
                        {"Çankaya", "Kavaklıdere", "Bahçelievler"},
                        {"Keçiören", "Etlik", "Aktepe"},
                        {"Yenimahalle", "Demetevler", "Batıkent"}
                });
        seedCity("Bursa", "16",
                new String[][]{
                        {"Nilüfer", "Görükle", "Fethiye"},
                        {"Osmangazi", "Hamitler", "Soğanlı"},
                        {"Yıldırım", "Davutdede", "Yavuz Selim"}
                });
        seedCity("Antalya", "7",
                new String[][]{
                        {"Muratpaşa", "Güzeloba", "Fener"},
                        {"Konyaaltı", "Liman", "Hurma"},
                        {"Kepez", "Varsak", "Emek"}
                });

        log.info("Location seed data created: 5 cities, 15 districts, 30 neighborhoods.");
    }

    private void seedCity(String cityName, String plateCode, String[][] districtData) {
        City city = new City();
        city.setName(cityName);
        city.setCode(plateCode);
        city.setIsActive(true);
        city = cityRepository.save(city);

        for (String[] districtEntry : districtData) {
            District district = new District();
            district.setCity(city);
            district.setName(districtEntry[0]);
            district.setIsActive(true);
            district = districtRepository.save(district);

            Neighborhood first = new Neighborhood();
            first.setDistrict(district);
            first.setName(districtEntry[1]);
            first.setIsActive(true);
            neighborhoodRepository.save(first);

            Neighborhood second = new Neighborhood();
            second.setDistrict(district);
            second.setName(districtEntry[2]);
            second.setIsActive(true);
            neighborhoodRepository.save(second);
        }
    }
}
