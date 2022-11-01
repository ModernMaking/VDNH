package ontology;

public class ObjectProperties {

    public static class Map
    {
        public static String STARTS_FROM = "http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#startsFrom";
        public static String FINISHES_TO = "http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#finishesTo";
        public static String IN_PLACE = "http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#inPlace";
        public static String HAS_FIRST_STATION = "http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#hasFirstStation";
        public static String HAS_NEXT_STATION = "http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#hasNextStation";
        public static String OF_ROUTE = "http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#ofRoute";
        public static String HAS_ACCORDING_STATION = "http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#hasAccordingStation";
        public static String HAS_FIRST_BUS_ROUTE_NODE="http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#hasFirstBusRouteNode";
        public static String HAS_NEXT_BUS_ROUTE_NODE="http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#hasNextBusRouteNode";
        public static String HAS_BUS_ROUTE_NODE = "http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#hasBusRouteNode";
        public static String HAS_NEAREST_BUS_STOP = "http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#hasNearestBusStop";
        public static String HAS_SIBLING_BIKE_NODE = "http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#hasSiblingBikeNode";
        public static String HAS_REACHABLE_BUS_ROUTE_NODE = "http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#hasReachableBusRouteNode";
    }

    public static class Route
    {
        public static String HAS_ACCORDING_OBJECT = "http://www.semanticweb.org/dns/ontologies/2022/8/route#hasAccordingObject";
        public static String HAS_PREVIOUS = "http://www.semanticweb.org/dns/ontologies/2022/8/route#hasPrevious";
        public static String HAS_NEXT = "http://www.semanticweb.org/dns/ontologies/2022/8/route#hasNext";
    }

    public static class Tag
    {
        public static String HAS_INTEREST_TAG = "http://www.semanticweb.org/dns/ontologies/2022/9/tag-ontology#hasInterestTag";
        public static String HAS_GEO_TAG = "http://www.semanticweb.org/dns/ontologies/2022/9/tag-ontology#hasGeoTag";
        public static String HAS_CHILD = "http://www.semanticweb.org/dns/ontologies/2022/9/tag-ontology#hasChild";
        public static String HAS_TAG1 = "http://www.semanticweb.org/dns/ontologies/2022/9/tag-ontology#hasTag1";
        public static String HAS_TAG2 = "http://www.semanticweb.org/dns/ontologies/2022/9/tag-ontology#hasTag2";
    }
}
