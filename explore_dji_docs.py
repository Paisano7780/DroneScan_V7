#!/usr/bin/env python3
"""
Script para explorar completamente la documentación DJI SDK Android v4
Extrae toda la estructura de clases, métodos y componentes de la API
"""

import requests
import json
import time
from urllib.parse import urljoin, urlparse
from bs4 import BeautifulSoup
import re

class DJIDocsExplorer:
    def __init__(self, base_url):
        self.base_url = base_url
        self.visited_urls = set()
        self.api_structure = {
            'components': {},
            'classes': {},
            'methods': {},
            'camera_modes': [],
            'media_management': {},
            'playback_management': {}
        }
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Linux; Android 10; DJI RC) AppleWebKit/537.36'
        })
    
    def is_valid_url(self, url):
        """Verificar si la URL es válida y pertenece al dominio DJI"""
        parsed = urlparse(url)
        return (parsed.netloc == 'developer.dji.com' and 
                'api-reference/android-api' in parsed.path)
    
    def fetch_page(self, url):
        """Obtener contenido de una página"""
        try:
            print(f"🔍 Explorando: {url}")
            response = self.session.get(url, timeout=10)
            response.raise_for_status()
            return response.text
        except Exception as e:
            print(f"❌ Error accediendo {url}: {e}")
            return None
    
    def extract_links(self, html, base_url):
        """Extraer todos los links relevantes de una página"""
        soup = BeautifulSoup(html, 'html.parser')
        links = set()
        
        for link in soup.find_all('a', href=True):
            href = link['href']
            full_url = urljoin(base_url, href)
            
            if self.is_valid_url(full_url):
                links.add(full_url)
        
        return links
    
    def analyze_camera_content(self, html, url):
        """Analizar contenido específico de cámara"""
        soup = BeautifulSoup(html, 'html.parser')
        
        # Buscar información sobre MediaManager
        if 'mediamanager' in url.lower() or 'media' in url.lower():
            print("📱 Encontrado contenido MediaManager")
            self.api_structure['media_management'][url] = {
                'methods': self.extract_methods(soup),
                'description': self.extract_description(soup)
            }
        
        # Buscar información sobre PlaybackManager
        if 'playback' in url.lower():
            print("🎮 Encontrado contenido PlaybackManager")
            self.api_structure['playback_management'][url] = {
                'methods': self.extract_methods(soup),
                'description': self.extract_description(soup)
            }
        
        # Buscar modos de cámara
        camera_modes = soup.find_all(text=re.compile(r'CameraMode\.|MEDIA_DOWNLOAD|PLAYBACK|SHOOT_PHOTO'))
        for mode in camera_modes:
            if mode.strip() not in self.api_structure['camera_modes']:
                self.api_structure['camera_modes'].append(mode.strip())
    
    def extract_methods(self, soup):
        """Extraer métodos de la página"""
        methods = []
        
        # Buscar métodos en diferentes formatos
        method_patterns = [
            soup.find_all('code'),
            soup.find_all('span', class_=re.compile(r'method|function')),
            soup.find_all(text=re.compile(r'void\s+\w+\(|boolean\s+\w+\(|\w+\s+\w+\(')),
        ]
        
        for pattern_group in method_patterns:
            for element in pattern_group:
                text = element.get_text() if hasattr(element, 'get_text') else str(element)
                if '(' in text and ')' in text:
                    methods.append(text.strip())
        
        return list(set(methods))
    
    def extract_description(self, soup):
        """Extraer descripción de la página"""
        # Buscar descripción en diferentes elementos
        desc_elements = soup.find_all(['p', 'div'], class_=re.compile(r'description|summary|overview'))
        
        descriptions = []
        for elem in desc_elements:
            text = elem.get_text().strip()
            if len(text) > 20:  # Filtrar textos muy cortos
                descriptions.append(text)
        
        return descriptions[:3]  # Primeras 3 descripciones
    
    def crawl_recursive(self, url, max_depth=3, current_depth=0):
        """Crawling recursivo de la documentación"""
        if (url in self.visited_urls or 
            current_depth > max_depth or 
            not self.is_valid_url(url)):
            return
        
        self.visited_urls.add(url)
        print(f"📖 Profundidad {current_depth}: {url}")
        
        html = self.fetch_page(url)
        if not html:
            return
        
        # Analizar contenido específico de cámara
        self.analyze_camera_content(html, url)
        
        # Extraer y seguir links
        if current_depth < max_depth:
            links = self.extract_links(html, url)
            print(f"🔗 Encontrados {len(links)} links en esta página")
            
            for link in links:
                time.sleep(0.5)  # Pausa para no sobrecargar el servidor
                self.crawl_recursive(link, max_depth, current_depth + 1)
    
    def save_results(self, filename='dji_api_structure.json'):
        """Guardar resultados en archivo JSON"""
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(self.api_structure, f, indent=2, ensure_ascii=False)
        print(f"💾 Resultados guardados en {filename}")
    
    def print_summary(self):
        """Imprimir resumen de los hallazgos"""
        print("\n" + "="*60)
        print("📊 RESUMEN DE EXPLORACIÓN DJI SDK")
        print("="*60)
        
        print(f"\n📱 MediaManager URLs encontradas: {len(self.api_structure['media_management'])}")
        for url in self.api_structure['media_management'].keys():
            print(f"   - {url}")
        
        print(f"\n🎮 PlaybackManager URLs encontradas: {len(self.api_structure['playback_management'])}")
        for url in self.api_structure['playback_management'].keys():
            print(f"   - {url}")
        
        print(f"\n📷 Modos de cámara encontrados: {len(set(self.api_structure['camera_modes']))}")
        for mode in set(self.api_structure['camera_modes']):
            print(f"   - {mode}")
        
        print(f"\n🔍 Total de URLs visitadas: {len(self.visited_urls)}")
        print("="*60)

def main():
    print("🚀 INICIANDO EXPLORACIÓN COMPLETA DE DJI SDK DOCS")
    print("="*60)
    
    # URLs principales para comenzar la exploración
    start_urls = [
        "https://developer.dji.com/api-reference/android-api/index.html",
        "https://developer.dji.com/api-reference/android-api/Components/Camera/index.html",
        "https://developer.dji.com/api-reference/android-api/Components/Camera/DJIMediaManager.html",
        "https://developer.dji.com/api-reference/android-api/Components/Camera/DJIPlaybackManager.html"
    ]
    
    explorer = DJIDocsExplorer("https://developer.dji.com")
    
    # Explorar cada URL inicial
    for url in start_urls:
        print(f"\n🎯 Comenzando exploración desde: {url}")
        try:
            explorer.crawl_recursive(url, max_depth=4)
        except Exception as e:
            print(f"❌ Error explorando {url}: {e}")
            continue
    
    # Guardar y mostrar resultados
    explorer.save_results('/workspaces/DroneScan_V7/dji_api_complete_structure.json')
    explorer.print_summary()
    
    print("\n✅ EXPLORACIÓN COMPLETADA!")
    print("📋 Revisa el archivo 'dji_api_complete_structure.json' para el análisis completo")

if __name__ == "__main__":
    main()
