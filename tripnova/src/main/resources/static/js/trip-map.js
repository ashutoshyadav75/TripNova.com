document.addEventListener("DOMContentLoaded", async function () {

    const mapElement = document.getElementById("tripMap");
    const statusElement = document.getElementById("mapStatus");

    if (!mapElement || typeof L === "undefined") {
        if (statusElement) {
            statusElement.textContent =
                "Map library load nahi hui. Internet connection check karein.";
        }
        return;
    }

    const destination = mapElement.dataset.destination?.trim();

    if (!destination) {
        statusElement.textContent = "Destination available nahi hai.";
        return;
    }

    // Initialize map
    const map = L.map("tripMap").setView([20.5937, 78.9629], 5);

    // OpenStreetMap tiles
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19,
        attribution:
            '&copy; <a href="https://www.openstreetmap.org/copyright">' +
            'OpenStreetMap</a> contributors'
    }).addTo(map);

    // Layers for different places
    const attractionLayer = L.layerGroup().addTo(map);
    const hotelLayer = L.layerGroup().addTo(map);
    const restaurantLayer = L.layerGroup().addTo(map);

    // Map layer controls
    L.control.layers(null, {
        "Tourist Attractions": attractionLayer,
        "Hotels & Stays": hotelLayer,
        "Restaurants & Cafes": restaurantLayer
    }, {
        collapsed: false
    }).addTo(map);

    // Create colored markers
    function createIcon(color, symbol) {
        return L.divIcon({
            className: "tripnova-map-marker",
            html: `
                <div style="
                    width: 34px;
                    height: 34px;
                    border-radius: 50%;
                    background: ${color};
                    color: white;
                    border: 3px solid white;
                    box-shadow: 0 2px 8px #0004;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    font-size: 17px;
                    font-weight: bold;
                ">${symbol}</div>
            `,
            iconSize: [34, 34],
            iconAnchor: [17, 17],
            popupAnchor: [0, -18]
        });
    }

    const icons = {
        destination: createIcon("#2563eb", "✈"),
        attraction: createIcon("#7c3aed", "★"),
        hotel: createIcon("#ea580c", "H"),
        restaurant: createIcon("#16a34a", "R")
    };

    // Escape text before inserting it into a popup
    function escapeHtml(value) {
        return String(value ?? "").replace(/[&<>"']/g, function (char) {
            const entities = {
                "&": "&amp;",
                "<": "&lt;",
                ">": "&gt;",
                '"': "&quot;",
                "'": "&#39;"
            };

            return entities[char];
        });
    }

    try {

        statusElement.textContent =
            "Finding " + destination + "...";

        // Find destination coordinates
        const geocodeUrl =
            "https://nominatim.openstreetmap.org/search?format=jsonv2" +
            "&limit=1&q=" + encodeURIComponent(destination);

        const geoResponse = await fetch(geocodeUrl, {
            headers: {
                "Accept": "application/json"
            }
        });

        if (!geoResponse.ok) {
            throw new Error("Destination search failed.");
        }

        const locations = await geoResponse.json();

        if (!locations.length) {
            statusElement.textContent =
                "Destination nahi mila. Destination ka naam check karein.";
            return;
        }

        const place = locations[0];

        const latitude = Number(place.lat);
        const longitude = Number(place.lon);

        if (!Number.isFinite(latitude) ||
            !Number.isFinite(longitude)) {
            throw new Error("Invalid destination coordinates.");
        }

        // Center map on the destination
        map.setView([latitude, longitude], 13);

        // Add destination marker
        L.marker([latitude, longitude], {
            icon: icons.destination
        })
            .addTo(map)
            .bindPopup(
                "<strong>" + escapeHtml(destination) +
                "</strong><br>Your TripNova destination"
            )
            .openPopup();

        statusElement.textContent =
            "Destination mil gaya. Nearby places load ho rahe hain...";

        // Find nearby places using OpenStreetMap Overpass API
        const radius = 5000;

        const query = `
            [out:json][timeout:25];

            (
                nwr(around:${radius},${latitude},${longitude})
                    ["tourism"~"attraction|museum|viewpoint|zoo|theme_park"];

                nwr(around:${radius},${latitude},${longitude})
                    ["tourism"~"hotel|hostel|guest_house|motel"];

                nwr(around:${radius},${latitude},${longitude})
                    ["amenity"~"restaurant|cafe|fast_food"];
            );

            out center tags 100;
        `;

        const overpassResponse = await fetch(
            "https://overpass-api.de/api/interpreter",
            {
                method: "POST",
                headers: {
                    "Content-Type":
                        "application/x-www-form-urlencoded;charset=UTF-8"
                },
                body: "data=" + encodeURIComponent(query)
            }
        );

        if (!overpassResponse.ok) {
            throw new Error("Nearby places service is unavailable.");
        }

        const data = await overpassResponse.json();

        const bounds = L.latLngBounds([
            [latitude, longitude]
        ]);

        let attractions = 0;
        let hotels = 0;
        let restaurants = 0;

        // Display nearby places
        data.elements.forEach(function (item) {

            const lat = item.lat ?? item.center?.lat;
            const lon = item.lon ?? item.center?.lon;

            if (!Number.isFinite(Number(lat)) ||
                !Number.isFinite(Number(lon))) {
                return;
            }

            const tags = item.tags || {};

            const name =
                tags.name ||
                tags["name:en"] ||
                tags.brand ||
                "Unnamed place";

            const tourism = tags.tourism || "";
            const amenity = tags.amenity || "";

            let layer;
            let icon;
            let category;

            if ([
                "hotel",
                "hostel",
                "guest_house",
                "motel"
            ].includes(tourism)) {

                layer = hotelLayer;
                icon = icons.hotel;
                category = "Hotel / Accommodation";
                hotels++;

            } else if ([
                "restaurant",
                "cafe",
                "fast_food"
            ].includes(amenity)) {

                layer = restaurantLayer;
                icon = icons.restaurant;
                category = "Restaurant / Cafe";
                restaurants++;

            } else if ([
                "attraction",
                "museum",
                "viewpoint",
                "zoo",
                "theme_park"
            ].includes(tourism)) {

                layer = attractionLayer;
                icon = icons.attraction;
                category = "Tourist Attraction";
                attractions++;

            } else {
                return;
            }

            const marker = L.marker(
                [Number(lat), Number(lon)],
                { icon: icon }
            );

            marker.bindPopup(
                "<strong>" + escapeHtml(name) + "</strong>" +
                "<br>" + escapeHtml(category)
            );

            marker.addTo(layer);

            bounds.extend([Number(lat), Number(lon)]);
        });
		
	
		const directionsUrl =
		    "https://www.openstreetmap.org/directions?engine=fossgis_osrm_car&route=" +
		    encodeURIComponent(latitude + "," + longitude) +
		    "%3B" +
		    encodeURIComponent(Number(lat) + "," + Number(lon));

		marker.bindPopup(
		    "<strong>" + escapeHtml(name) + "</strong>" +
		    "<br>" + escapeHtml(category) +
		    '<br><a href="' + directionsUrl +
		    '" target="_blank" rel="noopener noreferrer">' +
		    "Get Directions</a>"
		);
		

        // Fit the map around the destination and nearby places
        if (data.elements.length > 0) {
            map.fitBounds(bounds, {
                padding: [35, 35],
                maxZoom: 14
            });
        }

        statusElement.textContent =
            "Map loaded for " + destination + ". " +
            attractions + " attractions, " +
            hotels + " hotels/stays and " +
            restaurants + " restaurants found nearby.";

    } catch (error) {

        console.error("TripNova map error:", error);

        statusElement.textContent =
            "Destination map available hai, lekin nearby places " +
            "load nahi ho sake. Thodi der baad dobara try karein.";
    }

    // Fix map sizing
    setTimeout(function () {
        map.invalidateSize();
    }, 300);

});