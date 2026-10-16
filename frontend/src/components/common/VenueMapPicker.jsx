import React, { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { MapPin, Navigation, Search, Trash2, Crosshair, AlertCircle, Info } from 'lucide-react';
import Button from './Button';
import Input from './Input';

export function formatCoordinates(lat, lng) {
  if (lat == null || lng == null || isNaN(lat) || isNaN(lng)) return 'Not configured';
  const latDir = lat >= 0 ? 'N' : 'S';
  const lngDir = lng >= 0 ? 'E' : 'W';
  return `${Math.abs(lat).toFixed(5)}° ${latDir}, ${Math.abs(lng).toFixed(5)}° ${lngDir}`;
}

const venuePinIcon = L.divIcon({
  className: 'custom-venue-pin',
  html: `<div style="
    display: flex;
    align-items: center;
    justify-content: center;
    width: 34px;
    height: 34px;
    background: #6366f1;
    border: 2.5px solid #ffffff;
    border-radius: 50% 50% 50% 0;
    transform: rotate(-45deg);
    box-shadow: 0 4px 12px rgba(99, 102, 241, 0.5);
  ">
    <div style="
      width: 10px;
      height: 10px;
      background: #ffffff;
      border-radius: 50%;
      transform: rotate(45deg);
    "></div>
  </div>`,
  iconSize: [34, 34],
  iconAnchor: [17, 34],
  popupAnchor: [0, -34],
});

export default function VenueMapPicker({
  latitude,
  longitude,
  onChange,
  readOnly = false,
  height = '300px',
}) {
  const mapContainerRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const markerRef = useRef(null);
  const circleRef = useRef(null);

  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [locating, setLocating] = useState(false);
  const [searchError, setSearchError] = useState('');

  // Local inputs for fine-tuning
  const [manualLat, setManualLat] = useState(latitude != null ? String(latitude) : '');
  const [manualLng, setManualLng] = useState(longitude != null ? String(longitude) : '');

  // Default campus center: New Delhi IIT / campus area [28.5450, 77.1926]
  const defaultCenter = [28.5450, 77.1926];
  const initialCenter =
    latitude != null && longitude != null && !isNaN(latitude) && !isNaN(longitude)
      ? [Number(latitude), Number(longitude)]
      : defaultCenter;

  const updatePosition = (lat, lng, panMap = true) => {
    if (lat == null || lng == null || isNaN(lat) || isNaN(lng)) {
      if (markerRef.current) {
        markerRef.current.remove();
        markerRef.current = null;
      }
      if (circleRef.current) {
        circleRef.current.remove();
        circleRef.current = null;
      }
      setManualLat('');
      setManualLng('');
      if (onChange) onChange({ latitude: null, longitude: null });
      return;
    }

    const numLat = parseFloat(Number(lat).toFixed(6));
    const numLng = parseFloat(Number(lng).toFixed(6));
    setManualLat(String(numLat));
    setManualLng(String(numLng));

    if (onChange) {
      onChange({ latitude: numLat, longitude: numLng });
    }

    if (!mapInstanceRef.current) return;

    const map = mapInstanceRef.current;
    const latLng = [numLat, numLng];

    if (markerRef.current) {
      markerRef.current.setLatLng(latLng);
    } else {
      const marker = L.marker(latLng, {
        icon: venuePinIcon,
        draggable: !readOnly,
      }).addTo(map);

      if (!readOnly) {
        marker.on('dragend', (e) => {
          const pos = e.target.getLatLng();
          updatePosition(pos.lat, pos.lng, false);
        });
      }
      marker.bindPopup(`<strong>Venue Center</strong><br/>300m Attendance Perimeter`);
      markerRef.current = marker;
    }

    if (circleRef.current) {
      circleRef.current.setLatLng(latLng);
    } else {
      const circle = L.circle(latLng, {
        radius: 300,
        color: '#6366f1',
        fillColor: '#6366f1',
        fillOpacity: 0.15,
        weight: 2,
        dashArray: '6, 6',
      }).addTo(map);
      circle.bindTooltip('300m Geofence Radius', { permanent: false, direction: 'top' });
      circleRef.current = circle;
    }

    if (panMap) {
      map.setView(latLng, Math.max(map.getZoom(), 16));
    }
  };

  // Initialize Map
  useEffect(() => {
    if (!mapContainerRef.current) return;

    if (!mapInstanceRef.current) {
      const map = L.map(mapContainerRef.current, {
        center: initialCenter,
        zoom: latitude != null && longitude != null ? 16 : 14,
        zoomControl: true,
      });

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors',
        maxZoom: 19,
      }).addTo(map);

      if (!readOnly) {
        map.on('click', (e) => {
          updatePosition(e.latlng.lat, e.latlng.lng, false);
        });
      }

      mapInstanceRef.current = map;

      // Invalidate size to handle modal animation delay
      setTimeout(() => {
        map.invalidateSize();
      }, 250);

      // Place initial marker if coordinates provided
      if (latitude != null && longitude != null && !isNaN(latitude) && !isNaN(longitude)) {
        updatePosition(latitude, longitude, false);
      }
    }

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
        markerRef.current = null;
        circleRef.current = null;
      }
    };
  }, []);

  // Sync prop changes if external
  useEffect(() => {
    if (latitude != null && longitude != null) {
      setManualLat(String(latitude));
      setManualLng(String(longitude));
      if (mapInstanceRef.current) {
        const currentMarker = markerRef.current?.getLatLng();
        if (!currentMarker || currentMarker.lat !== latitude || currentMarker.lng !== longitude) {
          updatePosition(latitude, longitude, false);
        }
      }
    } else if (latitude == null && longitude == null) {
      setManualLat('');
      setManualLng('');
      if (markerRef.current) {
        markerRef.current.remove();
        markerRef.current = null;
      }
      if (circleRef.current) {
        circleRef.current.remove();
        circleRef.current = null;
      }
    }
  }, [latitude, longitude]);

  // Handle Search using OpenStreetMap Nominatim
  const handleSearch = async (e) => {
    e?.preventDefault();
    if (!searchQuery.trim()) return;
    setSearching(true);
    setSearchError('');
    setSearchResults([]);

    try {
      const res = await fetch(
        `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(
          searchQuery.trim()
        )}&limit=5`,
        {
          headers: {
            'Accept-Language': 'en',
          },
        }
      );
      const data = await res.json();
      if (data && data.length > 0) {
        setSearchResults(data);
      } else {
        setSearchError('No matching locations found. Try a broader search.');
      }
    } catch (err) {
      setSearchError('Search failed. Please check network connection.');
    } finally {
      setSearching(false);
    }
  };

  const handleSelectSearchResult = (result) => {
    const lat = parseFloat(result.lat);
    const lon = parseFloat(result.lon);
    updatePosition(lat, lon, true);
    setSearchResults([]);
    setSearchQuery(result.display_name.split(',')[0]);
  };

  // Handle Geolocation to Center on Current Location
  const handleUseCurrentLocation = () => {
    if (!navigator.geolocation) {
      alert('Geolocation is not supported by your browser');
      return;
    }
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setLocating(false);
        updatePosition(pos.coords.latitude, pos.coords.longitude, true);
      },
      (err) => {
        setLocating(false);
        alert(`Failed to get device location: ${err.message}`);
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  };

  // Handle Manual Lat/Lng Input Apply
  const handleApplyManualCoords = () => {
    const lat = parseFloat(manualLat);
    const lng = parseFloat(manualLng);
    if (isNaN(lat) || lat < -90 || lat > 90) {
      alert('Latitude must be a valid number between -90 and 90');
      return;
    }
    if (isNaN(lng) || lng < -180 || lng > 180) {
      alert('Longitude must be a valid number between -180 and 180');
      return;
    }
    updatePosition(lat, lng, true);
  };

  const handleClear = () => {
    updatePosition(null, null, false);
  };

  const hasCoords = latitude != null && longitude != null && !isNaN(latitude) && !isNaN(longitude);

  return (
    <div className="space-y-3">
      {/* Search Bar & Quick Location Buttons */}
      {!readOnly && (
        <div className="space-y-2">
          <div className="flex gap-2">
            <div className="relative flex-1">
              <input
                type="text"
                placeholder="Search campus building, address, or landmark..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    handleSearch();
                  }
                }}
                className="w-full rounded-xl bg-slate-950 border border-slate-800 pl-9 pr-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
              <Search className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
            </div>

            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={handleSearch}
              loading={searching}
            >
              Search
            </Button>

            <Button
              type="button"
              variant="outline"
              size="sm"
              icon={Crosshair}
              onClick={handleUseCurrentLocation}
              loading={locating}
              title="Locate device GPS"
            >
              My GPS
            </Button>
          </div>

          {/* Search Results Dropdown */}
          {searchResults.length > 0 && (
            <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden divide-y divide-slate-800/80 shadow-xl max-h-40 overflow-y-auto z-50">
              {searchResults.map((item, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => handleSelectSearchResult(item)}
                  className="w-full px-3 py-2 text-left text-xs text-slate-300 hover:bg-slate-800/80 transition-colors flex items-start gap-2"
                >
                  <MapPin className="w-3.5 h-3.5 text-indigo-400 mt-0.5 shrink-0" />
                  <span className="truncate">{item.display_name}</span>
                </button>
              ))}
            </div>
          )}

          {searchError && (
            <p className="text-[11px] text-rose-400 flex items-center gap-1">
              <AlertCircle className="w-3 h-3" />
              {searchError}
            </p>
          )}
        </div>
      )}

      {/* Map Container */}
      <div className="relative rounded-xl overflow-hidden border border-slate-800 bg-slate-950">
        <div ref={mapContainerRef} style={{ height, width: '100%', zIndex: 10 }} />

        {/* Overlay instruction */}
        {!readOnly && !hasCoords && (
          <div className="absolute top-2 left-2 z-20 pointer-events-none bg-slate-950/90 border border-slate-800/90 rounded-lg px-2.5 py-1 text-[11px] text-indigo-300 shadow">
            Click anywhere on the map to place the venue pin
          </div>
        )}

        {hasCoords && (
          <div className="absolute bottom-2 left-2 z-20 pointer-events-none bg-slate-950/90 backdrop-blur border border-slate-800/90 rounded-lg px-2.5 py-1 text-[11px] text-emerald-400 font-mono shadow flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            300m Attendance Geofence Active
          </div>
        )}
      </div>

      {/* Coordinate Display and Fine-Tuning */}
      <div className="space-y-2 pt-1">
        <div className="flex items-center justify-between text-xs">
          <span className="text-slate-400 font-medium flex items-center gap-1.5">
            <Navigation className="w-3.5 h-3.5 text-indigo-400" />
            Selected Venue Coordinates:
          </span>
          <span className="font-mono font-semibold text-white">
            {formatCoordinates(latitude, longitude)}
          </span>
        </div>

        {!readOnly && (
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 items-end pt-1">
            <div>
              <label className="block text-[10px] font-semibold uppercase tracking-wider text-slate-400 mb-1">
                Latitude (-90 to 90)
              </label>
              <input
                type="number"
                step="0.000001"
                placeholder="e.g. 28.545000"
                value={manualLat}
                onChange={(e) => setManualLat(e.target.value)}
                className="w-full rounded-lg bg-slate-950 border border-slate-800 px-2.5 py-1.5 text-xs text-slate-100 font-mono focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>

            <div>
              <label className="block text-[10px] font-semibold uppercase tracking-wider text-slate-400 mb-1">
                Longitude (-180 to 180)
              </label>
              <input
                type="number"
                step="0.000001"
                placeholder="e.g. 77.192600"
                value={manualLng}
                onChange={(e) => setManualLng(e.target.value)}
                className="w-full rounded-lg bg-slate-950 border border-slate-800 px-2.5 py-1.5 text-xs text-slate-100 font-mono focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>

            <div className="flex gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="flex-1"
                onClick={handleApplyManualCoords}
                disabled={!manualLat || !manualLng}
              >
                Apply
              </Button>

              {hasCoords && (
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  icon={Trash2}
                  onClick={handleClear}
                  className="text-rose-400 hover:text-rose-300"
                  title="Clear coordinates"
                >
                  Clear
                </Button>
              )}
            </div>
          </div>
        )}

        <div className="p-2 rounded-lg bg-indigo-950/30 border border-indigo-800/40 text-[11px] text-indigo-300 flex items-start gap-1.5">
          <Info className="w-3.5 h-3.5 text-indigo-400 shrink-0 mt-0.5" />
          <span>
            During QR attendance check-in, participants must be physically located within 300 meters of this venue coordinate.
          </span>
        </div>
      </div>
    </div>
  );
}
