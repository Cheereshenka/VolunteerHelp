package ru.coursework.volunteerhelp;

import org.junit.jupiter.api.Test;
import ru.coursework.volunteerhelp.service.MatchingPolicy;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class MatchingPolicyTest {
    @Test void categoryMatchesCaseInsensitive(){assertTrue(MatchingPolicy.containsCategory("покупки, уборка", "УБОРКА"));}
    @Test void categoryDoesNotMatch(){assertFalse(MatchingPolicy.containsCategory("покупки, уборка", "доставка"));}
    @Test void availabilityInsideInterval(){assertTrue(MatchingPolicy.isAvailable(LocalTime.of(9,0),LocalTime.of(18,0),LocalTime.of(10,0),LocalTime.of(17,0)));}
    @Test void availabilityAtBoundaries(){assertTrue(MatchingPolicy.isAvailable(LocalTime.of(9,0),LocalTime.of(18,0),LocalTime.of(9,0),LocalTime.of(18,0)));}
    @Test void availabilityRejectsOutside(){assertFalse(MatchingPolicy.isAvailable(LocalTime.of(9,0),LocalTime.of(18,0),LocalTime.of(8,59),LocalTime.of(17,0)));}
    @Test void availabilityRejectsEmpty(){assertFalse(MatchingPolicy.isAvailable(LocalTime.of(9,0),LocalTime.of(18,0),LocalTime.of(12,0),LocalTime.of(12,0)));}
    @Test void overlapAtBoundaryIsFalse(){assertFalse(MatchingPolicy.overlaps(LocalTime.of(10,0),LocalTime.of(12,0),LocalTime.of(12,0),LocalTime.of(14,0)));}
    @Test void overlapInsideIsTrue(){assertTrue(MatchingPolicy.overlaps(LocalTime.of(10,0),LocalTime.of(13,0),LocalTime.of(12,0),LocalTime.of(14,0)));}
}
