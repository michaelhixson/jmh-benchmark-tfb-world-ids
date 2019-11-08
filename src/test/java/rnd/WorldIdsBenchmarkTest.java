package rnd;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static rnd.WorldIdsBenchmark.MAX_WORLD_NUMBER_PLUS_ONE;
import static rnd.WorldIdsBenchmark.MIN_WORLD_NUMBER;

import com.google.common.collect.Range;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.openjdk.jmh.annotations.Benchmark;
import rnd.WorldIdsBenchmark.World;

/**
 * Tests for {@link WorldIdsBenchmark}.
 */
public final class WorldIdsBenchmarkTest {
  /**
   * Verifies that all {@code World[]}-returning benchmark methods have the
   * correct behavior.
   */
  @TestFactory
  public Stream<DynamicTest> testBenchmarkMethods() {

    var methods = Arrays.stream(WorldIdsBenchmark.class.getDeclaredMethods())
                        .filter(method -> method.getAnnotation(Benchmark.class) != null)
                        .filter(method -> method.getParameterCount() == 0)
                        .filter(method -> method.getReturnType() == World[].class)
                        .collect(toList());

    if (methods.isEmpty()) {
      throw new RuntimeException(
          "No benchmark methods found... does this test need to be updated?");
    }

    return DynamicTest.stream(
        methods.iterator(),
        method -> method.getName(),
        method -> {

          var benchmark = new WorldIdsBenchmark();
          // TODO: Test other values for queries, especially zero.
          benchmark.queries = 20;

          World[] worlds = (World[]) method.invoke(benchmark);

          assertNotNull(
              worlds,
              "method must not return null");

          assertEquals(
              benchmark.queries,
              worlds.length,
              "method must return the expected number of worlds");

          assertEquals(
              0,
              Arrays.stream(worlds)
                    .filter(world -> world == null)
                    .count(),
              "method must not return null elements");

          assertEquals(
              benchmark.queries,
              Arrays.stream(worlds)
                    .map(world -> world.id)
                    .collect(toSet())
                    .size(),
              "method must return worlds with distinct ids");

          var range = Range.closedOpen(MIN_WORLD_NUMBER,
                                       MAX_WORLD_NUMBER_PLUS_ONE);

          for (var i = 0; i < worlds.length; i++) {
            var world = worlds[i];

            assertTrue(
                range.contains(world.id),
                "method must return worlds with id values in range "
                    + range
                    + ", but the world at index "
                    + i
                    + " had an id of "
                    + world.id);

            assertTrue(
                range.contains(world.randomNumber),
                "method must return worlds with randomNumber values in range "
                    + range
                    + ", but the world at index "
                    + i
                    + " had a randomNumber of "
                    + world.randomNumber);
          }
        });
  }
}
