# Capturas de cobertura

Aquí van las capturas del reporte de cobertura de **cada** microservicio, enlazadas desde el
`README.md` raíz.

Para generarlas:

**Desde el IDE** (es la forma que pide la rúbrica) — IntelliJ: clic derecho sobre
`src/test/kotlin` → *Run tests with Coverage*, y captura la ventana *Coverage*. Hazlo una vez
en `users/` y otra en `matchpoint/`.

**Con JaCoCo** (automatizado, mismo número):

```bash
cd users && ./gradlew test && open build/reports/jacoco/test/html/index.html
```

```bash
cd matchpoint && ./gradlew test && open build/reports/jacoco/test/html/index.html
```

Guarda las imágenes aquí como `users-coverage.png` y `matchpoint-coverage.png`.
