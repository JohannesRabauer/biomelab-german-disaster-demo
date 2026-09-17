/* CrisisScope dashboard client behavior: the MapLibre map, its DOM markers,
 * relative-time labels and the glue between the filter form, the HTMX feed
 * swap and the map refresh. No build step — plain ES2020 for browser use. */
(function () {
  'use strict';

  const mapEl = document.getElementById('map');
  if (!mapEl) return;

  const map = new maplibregl.Map({
    container: 'map',
    style: 'https://demotiles.maplibre.org/style.json',
    center: [Number(mapEl.dataset.centerLng), Number(mapEl.dataset.centerLat)],
    zoom: Number(mapEl.dataset.zoom),
    attributionControl: { compact: true },
  });
  map.addControl(new maplibregl.NavigationControl({ showCompass: false }), 'top-right');
  window.crisisMap = map;

  let markers = [];

  function clearMarkers() {
    markers.forEach((marker) => marker.remove());
    markers = [];
  }

  function severityClass(severity) {
    return 'severity-' + String(severity || '').toLowerCase();
  }

  function popupHtml(props) {
    const sourceLink = props.url
      ? `<a href="${props.url}" target="_blank" rel="noopener" class="text-sky-400 hover:text-sky-300 text-xs font-medium">View source ↗</a>`
      : '';
    return `
      <div class="space-y-1.5 max-w-[240px]">
        <div class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide badge-severity-${(props.severity || '').toLowerCase()} inline-flex px-2 py-0.5 rounded-full border">
          ${props.icon || ''} ${props.severityLabel}
        </div>
        <p class="font-semibold text-sm leading-snug text-slate-100">${props.title}</p>
        <p class="text-xs text-slate-400">${props.locationName}, ${props.region}</p>
        ${sourceLink}
      </div>`;
  }

  function renderMarkers(geojson) {
    clearMarkers();
    geojson.features.forEach((feature) => {
      const props = feature.properties;
      const el = document.createElement('div');
      el.className = 'map-marker ' + severityClass(props.severity);
      el.style.backgroundColor = 'var(--severity-' + severityClass(props.severity).split('-')[1] + ')';
      el.title = props.title;

      const popup = new maplibregl.Popup({ offset: 14, closeButton: false }).setHTML(popupHtml(props));

      const marker = new maplibregl.Marker({ element: el })
        .setLngLat(feature.geometry.coordinates)
        .setPopup(popup)
        .addTo(map);

      el.addEventListener('click', () => loadRegionDetail(props.region));
      markers.push(marker);
    });
  }

  function currentFilterParams() {
    const form = document.getElementById('filter-form');
    return new URLSearchParams(new FormData(form)).toString();
  }

  function refreshMap() {
    fetch('/api/events?' + currentFilterParams())
      .then((response) => response.json())
      .then(renderMarkers)
      .catch((err) => console.error('CrisisScope: failed to refresh map data', err));
  }

  function loadRegionDetail(region) {
    htmx.ajax('GET', '/fragments/region-detail?region=' + encodeURIComponent(region), {
      target: '#region-detail',
      swap: 'innerHTML',
    });
  }
  window.loadRegionDetail = loadRegionDetail;

  map.on('load', refreshMap);

  const filterForm = document.getElementById('filter-form');
  if (filterForm) {
    filterForm.addEventListener('change', refreshMap);
  }

  const resetButton = document.getElementById('filter-reset');
  if (resetButton) {
    resetButton.addEventListener('click', () => {
      filterForm.reset();
      htmx.trigger(filterForm, 'change');
      refreshMap();
    });
  }

  // Relative time labels ("5m ago") for every element carrying a timestamp,
  // re-run after each HTMX swap since new .event-time nodes may have landed.
  function formatRelativeTime(iso) {
    const diffMs = Date.now() - new Date(iso).getTime();
    const minutes = Math.round(diffMs / 60000);
    if (minutes < 1) return 'just now';
    if (minutes < 60) return minutes + 'm ago';
    const hours = Math.round(minutes / 60);
    if (hours < 24) return hours + 'h ago';
    const days = Math.round(hours / 24);
    return days + 'd ago';
  }

  function refreshRelativeTimes(scope) {
    (scope || document).querySelectorAll('.event-time[data-timestamp]').forEach((el) => {
      el.textContent = formatRelativeTime(el.dataset.timestamp);
    });
  }

  document.body.addEventListener('htmx:afterSwap', (evt) => refreshRelativeTimes(evt.detail.target));
  refreshRelativeTimes();
  setInterval(() => refreshRelativeTimes(), 30000);

  // Live clock in the top nav.
  const clockEl = document.getElementById('nav-clock');
  if (clockEl) {
    const formatter = new Intl.DateTimeFormat('de-DE', {
      timeZone: 'Europe/Berlin',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
    const tick = () => { clockEl.textContent = formatter.format(new Date()) + ' CET'; };
    tick();
    setInterval(tick, 1000);
  }
})();
