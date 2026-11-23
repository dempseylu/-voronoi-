package org.example.voronoi.algorithm;

import org.example.voronoi.geometry.Coordinate;
import org.example.voronoi.model.UserRecord;
import org.example.voronoi.model.VoronoiCell;

import java.util.List;

/**
 * Assigns every user record to a Voronoi cell and derives hourly statistics.
 *
 * <p>Containment is decided by the polygon itself, not by an area-difference
 * heuristic. Users outside every cell are assigned to the nearest station and
 * counted separately, so the result never silently claims full coverage.</p>
 */
public final class PopulationAnalyzer {

    public Result analyze(List<VoronoiCell> cells, List<UserRecord> users) {
        long inside = 0;
        long nearest = 0;
        long unassigned = 0;
        long unknownHour = 0;

        for (UserRecord user : users) {
            Coordinate position = user.position();
            int hour = user.hourOfDay();
            if (hour < 0) {
                unknownHour++;
            }

            VoronoiCell match = findContaining(cells, position);
            if (match != null) {
                inside++;
            } else {
                match = findNearest(cells, position);
                if (match != null) {
                    nearest++;
                }
            }

            if (match == null) {
                unassigned++;
            } else {
                match.addUser(hour);
            }
        }

        for (VoronoiCell cell : cells) {
            cell.computeCapacity();
            cell.computeChangeRate();
        }
        return new Result(inside, nearest, unassigned, unknownHour, users.size());
    }

    private VoronoiCell findContaining(List<VoronoiCell> cells, Coordinate point) {
        for (VoronoiCell cell : cells) {
            if (cell.contains(point)) {
                return cell;
            }
        }
        return null;
    }

    private VoronoiCell findNearest(List<VoronoiCell> cells, Coordinate point) {
        VoronoiCell best = null;
        double bestDistance = Double.MAX_VALUE;
        for (VoronoiCell cell : cells) {
            double distance = cell.getStation().position().distanceTo(point);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = cell;
            }
        }
        return best;
    }

    /** Outcome of one analysis run. */
    public static final class Result {

        private final long insideCells;
        private final long assignedToNearest;
        private final long unassigned;
        private final long unknownHour;
        private final int totalUsers;

        Result(long insideCells, long assignedToNearest, long unassigned, long unknownHour, int totalUsers) {
            this.insideCells = insideCells;
            this.assignedToNearest = assignedToNearest;
            this.unassigned = unassigned;
            this.unknownHour = unknownHour;
            this.totalUsers = totalUsers;
        }

        public long getInsideCells() {
            return insideCells;
        }

        public long getAssignedToNearest() {
            return assignedToNearest;
        }

        public long getUnassigned() {
            return unassigned;
        }

        public long getUnknownHour() {
            return unknownHour;
        }

        public int getTotalUsers() {
            return totalUsers;
        }

        @Override
        public String toString() {
            return "users=" + totalUsers
                    + ", inside=" + insideCells
                    + ", nearest=" + assignedToNearest
                    + ", unassigned=" + unassigned
                    + ", unknownHour=" + unknownHour;
        }
    }
}
