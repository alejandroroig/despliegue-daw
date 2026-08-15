FROM squidfunk/mkdocs-material:latest

# La imagen oficial no trae el plugin que incrusta las diapositivas en PDF.
RUN pip install --no-cache-dir mkdocs-pdf
