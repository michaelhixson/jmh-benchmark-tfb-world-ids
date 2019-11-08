package rnd;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.BitSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
public class WorldIdsBenchmark {

  public static void main(String[] args) throws Exception {
    Options options = new OptionsBuilder()
        .include(WorldIdsBenchmark.class.getName())
        .build();
    new Runner(options).run();
  }

  public static final class World {
    public final int id;
    public final int randomNumber;

    public World(int id, int randomNumber) {
      this.id = id;
      this.randomNumber = randomNumber;
    }
  }

  @VisibleForTesting
  static final int MIN_WORLD_NUMBER = 1;

  @VisibleForTesting
  static final int MAX_WORLD_NUMBER_PLUS_ONE = 10_001;

  private static int randomWorldNumber() {
    return ThreadLocalRandom.current().nextInt(MIN_WORLD_NUMBER,
                                               MAX_WORLD_NUMBER_PLUS_ONE);
  }

  private static World getWorld(int id) {
    // stand-in for database query
    return new World(id, randomWorldNumber());
  }

  @VisibleForTesting
  @Param("20")
  int queries;

  @Benchmark
  public World[] atomicCounter() {
    World[] worlds = new World[queries];
    AtomicInteger i = new AtomicInteger(0);
    ThreadLocalRandom
        .current()
        .ints(MIN_WORLD_NUMBER, MAX_WORLD_NUMBER_PLUS_ONE)
        .distinct()
        .limit(queries)
        .forEach(id -> worlds[i.getAndAdd(1)] = getWorld(id));
    return worlds;
  }

  @Benchmark
  public World[] arrayCounter() {
    World[] worlds = new World[queries];
    int[] i = { 0 };
    ThreadLocalRandom
        .current()
        .ints(MIN_WORLD_NUMBER, MAX_WORLD_NUMBER_PLUS_ONE)
        .distinct()
        .limit(queries)
        .forEach(id -> worlds[i[0]++] = getWorld(id));
    return worlds;
  }

  @Benchmark
  public World[] streamOnly() {
    return ThreadLocalRandom
        .current()
        .ints(MIN_WORLD_NUMBER, MAX_WORLD_NUMBER_PLUS_ONE)
        .distinct()
        .limit(queries)
        .mapToObj(id -> getWorld(id))
        .toArray(World[]::new);
  }

  @Benchmark
  public World[] primitiveSet() {
    IntSet ids = new IntOpenHashSet(queries);
    World[] worlds = new World[queries];
    for (int i = 0; i < queries; i++) {
      int id;
      do {
        id = randomWorldNumber();
      } while (!ids.add(id));
      worlds[i] = getWorld(id);
    }
    return worlds;
  }

  @Benchmark
  public World[] boxedSet() {
    // Use Guava to correctly size the HashSet.
    Set<Integer> ids = Sets.newHashSetWithExpectedSize(queries);
    World[] worlds = new World[queries];
    for (int i = 0; i < queries; i++) {
      int id;
      do {
        id = randomWorldNumber();
      } while (!ids.add(id));
      worlds[i] = getWorld(id);
    }
    return worlds;
  }

  @Benchmark
  public World[] bitSet() {
    BitSet ids = new BitSet(MAX_WORLD_NUMBER_PLUS_ONE);
    World[] worlds = new World[queries];
    for (int i = 0; i < queries; i++) {
      int id;
      do {
        id = randomWorldNumber();
      } while (ids.get(id));
      ids.set(id);
      worlds[i] = getWorld(id);
    }
    return worlds;
  }

  @Benchmark
  public World[] noSet() {
    World[] worlds = new World[queries];
    if (queries == 0) return worlds;
    outerLoop:
    for (int i = 0;;) {
      int id = randomWorldNumber();
      for (int j = 0; j < i; j++) {
        if (worlds[j].id == id) {
          continue outerLoop;
        }
      }
      worlds[i] = getWorld(id);
      if (++i == queries) {
        return worlds;
      }
    }
  }
}
