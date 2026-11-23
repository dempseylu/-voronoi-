package org.example.voronoi;

import org.example.voronoi.algorithm.DelaunayTriangulator;
import org.example.voronoi.algorithm.PopulationAnalyzer;
import org.example.voronoi.algorithm.Triangulation;
import org.example.voronoi.algorithm.VoronoiBuilder;
import org.example.voronoi.geometry.Coordinate;
import org.example.voronoi.model.Station;
import org.example.voronoi.model.UserRecord;
import org.example.voronoi.model.VoronoiCell;

import java.util.ArrayList;
import java.util.List;

/**
 * The end-to-end calculation, free of file and database concerns so that it can
 * be exercised directly from tests.
 */
public final class Pipeline {

    public Result run(List<Station> stations, List<UserRecord> users, double clipMarginRatio) {
        if (stations == null || stations.isEmpty()) {
            throw new IllegalArgumentException("At least one station is required");
        }

        List<Coordinate> positions = new ArrayList<>(stations.size());
        for (Station station : stations) {
            positions.add(station.position());
        }

        Triangulation triangulation = new DelaunayTriangulator().triangulate(positions);
        VoronoiBuilder builder = new VoronoiBuilder(clipMarginRatio);
        List<VoronoiCell> cells = builder.build(triangulation, stations);
        PopulationAnalyzer.Result population = new PopulationAnalyzer()
                .analyze(cells, users == null ? new ArrayList<UserRecord>() : users);

        return new Result(triangulation, cells, population, builder.getDuplicateSitesSkipped());
    }

    /** Everything the caller may want to report or export. */
    public static final class Result {

        private final Triangulation triangulation;
        private final List<VoronoiCell> cells;
        private final PopulationAnalyzer.Result population;
        private final int duplicateSitesSkipped;

        Result(Triangulation triangulation, List<VoronoiCell> cells,
               PopulationAnalyzer.Result population, int duplicateSitesSkipped) {
            this.triangulation = triangulation;
            this.cells = cells;
            this.population = population;
            this.duplicateSitesSkipped = duplicateSitesSkipped;
        }

        public Triangulation getTriangulation() {
            return triangulation;
        }

        public List<VoronoiCell> getCells() {
            return cells;
        }

        public PopulationAnalyzer.Result getPopulation() {
            return population;
        }

        public int getDuplicateSitesSkipped() {
            return duplicateSitesSkipped;
        }

        public double totalAreaSquareKilometres() {
            double sum = 0.0;
            for (VoronoiCell cell : cells) {
                sum += cell.getAreaSquareKilometres();
            }
            return sum;
        }
    }
}
