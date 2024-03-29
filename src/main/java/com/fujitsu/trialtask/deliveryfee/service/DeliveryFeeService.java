package com.fujitsu.trialtask.deliveryfee.service;

import com.fujitsu.trialtask.deliveryfee.dto.DeliveryFeeDto;
import com.fujitsu.trialtask.deliveryfee.dto.WeatherMeasurementDto;
import com.fujitsu.trialtask.deliveryfee.entity.*;
import com.fujitsu.trialtask.deliveryfee.repository.*;
import com.fujitsu.trialtask.deliveryfee.util.enums.WeatherCode;
import com.fujitsu.trialtask.deliveryfee.util.exception.DeliveryFeeException;
import com.fujitsu.trialtask.deliveryfee.util.exception.CodeItemException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeliveryFeeService {
    private final ExtraFeeRepository extraFeeRepository;
    private final CodeItemRepository codeItemRepository;
    private final RegionalBaseFeeRepository baseFeeRepository;
    private final VehicleRepository vehicleRepository;
    private final CityRepository cityRepository;
    private final WorkProhibitionRepository workProhibitionRepository;
    private final WeatherService weatherService;

    public DeliveryFeeDto getDeliveryFee(Long cityId, Long vehicleId) throws DeliveryFeeException {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new DeliveryFeeException("Invalid vehicle ID", DeliveryFeeException.Reason.INVALID_VEHICLE_ID);
        }

        final City city = cityRepository.findById(cityId).orElseThrow(
                () -> new DeliveryFeeException("Invalid City ID", DeliveryFeeException.Reason.INVALID_CITY_ID)
        );
        final WeatherMeasurementDto measurement = weatherService.getLatestMeasurementFromStation(city.getWMOcode());
        final List<WeatherCode> weatherCodes = getWeatherCodes(measurement);

        if (unfitWeatherConditions(vehicleId, weatherCodes)) {
            throw new DeliveryFeeException("Usage of selected vehicle type is forbidden", DeliveryFeeException.Reason.UNFIT_WEATHER_CONDITIONS);
        }

        final BigDecimal baseFee = getBaseFeeAmount(cityId, vehicleId);
        final BigDecimal extraFee = getExtraFees(weatherCodes, vehicleId);
        final BigDecimal totalFee = baseFee.add(extraFee);
        return new DeliveryFeeDto(cityId, vehicleId, baseFee, extraFee, totalFee);
    }

    private List<WeatherCode> getWeatherCodes(WeatherMeasurementDto measurement) {
        final List<WeatherCode> codes = new ArrayList<>();
        final Optional<WeatherCode> airTempCode = getAirTemperatureCode(measurement);
        final Optional<WeatherCode> windSpeedCode = getWindSpeedCode(measurement);
        final Optional<WeatherCode> phenomenonCode = getWeatherPhenomenonCode(measurement);
        airTempCode.ifPresent(codes::add);
        windSpeedCode.ifPresent(codes::add);
        phenomenonCode.ifPresent(codes::add);
        return codes;
    }

    private boolean unfitWeatherConditions(Long vehicleId, List<WeatherCode> weatherCodes) {
        for (WeatherCode code : weatherCodes) {
            try {
                final CodeItem codeItem = getWeatherCodeItem(code);
                final Optional<WorkProhibition> prohibition = workProhibitionRepository.findByVehicleIdAndCodeItemCode(vehicleId, codeItem.getCode());
                if (prohibition.isPresent()) {
                    return true;
                }
            } catch (CodeItemException e) {
                log.error(String.format("%s. Code: %s", e.getMessage(), e.getCode()));
            }
        }
        return false;
    }

    private BigDecimal getBaseFeeAmount(Long cityId, Long vehicleId) throws DeliveryFeeException {
        final RegionalBaseFee baseFee = baseFeeRepository.findByCityIdAndVehicleId(cityId, vehicleId)
                .orElseThrow(() -> new DeliveryFeeException("Invalid city and vehicle combination",
                        DeliveryFeeException.Reason.BASE_FEE_DOES_NOT_EXIST));
        return baseFee.getFeeAmount();
    }

    private BigDecimal getExtraFees(List<WeatherCode> weatherCodes, Long vehicleId) {
        BigDecimal total = BigDecimal.ZERO;
        for (WeatherCode code : weatherCodes) {
            try {
                final CodeItem codeItem = getWeatherCodeItem(code);
                final Optional<ExtraFee> extraFee = extraFeeRepository.findByVehicleIdAndCodeItemCode(vehicleId, codeItem.getCode());
                if (extraFee.isPresent()) {
                    total = total.add(extraFee.get().getFeeAmount());
                }
            } catch (CodeItemException e) {
                log.error(String.format("%s. Code: %s", e.getMessage(), e.getCode()));
            }
        }
        return total;
    }

    private Optional<WeatherCode> getWindSpeedCode(WeatherMeasurementDto measurement) {
        if (measurement.getWindSpeed() == null) {
            return Optional.empty();
        }

        final boolean speedTenToTwenty = 10 <= measurement.getWindSpeed() && measurement.getWindSpeed() <= 20;
        final boolean speedAboveTwenty = measurement.getWindSpeed() > 20;

        if (speedTenToTwenty) {
            return Optional.of(WeatherCode.WS_TEN_TO_TWENTY);
        } else if (speedAboveTwenty) {
            return Optional.of(WeatherCode.WS_ABOVE_TWENTY);
        }

        return Optional.empty();
    }

    private Optional<WeatherCode> getAirTemperatureCode(WeatherMeasurementDto measurement) {
        if (measurement.getAirTemperature() == null) {
            return Optional.empty();
        }

        final boolean underMinusTenDegrees = measurement.getAirTemperature() < -10;
        final boolean minusTenToZeroDegrees =
                -10 <= measurement.getAirTemperature() && measurement.getAirTemperature() <= 0;

        if (underMinusTenDegrees) {
            return Optional.of(WeatherCode.AT_UNDER_MINUS_TEN);
        } else if (minusTenToZeroDegrees) {
            return Optional.of(WeatherCode.AT_MINUS_TEN_TO_ZERO);
        }

        return Optional.empty();
    }

    private Optional<WeatherCode> getWeatherPhenomenonCode(WeatherMeasurementDto measurement) {
        if (measurement.getPhenomenon() == null) {
            return Optional.empty();
        }

        final String phenomenon = measurement.getPhenomenon().toLowerCase();
        final boolean rain = phenomenon.contains("rain");
        final boolean snowOrFleet = phenomenon.matches(".*fleet.*|.*snow.*");
        final boolean glazeOrHailOrThunder = phenomenon.matches(".*glaze.*|.*hail.*|.*thunder.*");

        if (rain) {
            return Optional.of(WeatherCode.WP_RAIN);
        } else if (snowOrFleet) {
            return Optional.of(WeatherCode.WP_SNOW_FLEET);
        } else if (glazeOrHailOrThunder) {
            return Optional.of(WeatherCode.WP_GLAZE_HAIL_THUNDER);
        }

        return Optional.empty();
    }

    private CodeItem getWeatherCodeItem(WeatherCode code) throws CodeItemException {
        return codeItemRepository.findByCode(code.name()).orElseThrow(
                () -> new CodeItemException("Code item does not exist.", code.name())
        );
    }
}
