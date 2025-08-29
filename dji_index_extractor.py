#!/usr/bin/env python3
"""
Etapa 1: Extractor de Links del Index Principal de DJI
Extrae todos los links de la página principal para procesamiento posterior
"""

import requests
import json
import time
from urllib.parse import urljoin, urlparse
from bs4 import BeautifulSoup
from datetime import datetime

class DJIIndexExtractor:
    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        })
        
        self.index_url = "https://developer.dji.com/api-reference/android-api/index.html"
        self.base_url = "https://developer.dji.com/api-reference/android-api/"
        
    def is_valid_dji_link(self, url):
        """Verificar si el link es válido para DJI docs"""
        if not url:
            return False
        
        parsed = urlparse(url)
        
        # Debe ser del dominio DJI y de android-api
        if parsed.netloc != 'developer.dji.com':
            return False
        
        if not parsed.path.startswith('/api-reference/android-api/'):
            return False
        
        # Filtrar links no deseados
        skip_patterns = [
            'javascript:', 'mailto:', '#', 'tel:',
            '.pdf', '.zip', '.jpg', '.png', '.gif', '.css', '.js'
        ]
        
        for pattern in skip_patterns:
            if pattern in url.lower():
                return False
        
        return True
    
    def categorize_link(self, url, text):
        """Categorizar el link según su contenido"""
        url_lower = url.lower()
        text_lower = text.lower() if text else ""
        
        categories = {
            'camera': ['camera', 'djicamera'],
            'mediamanager': ['mediamanager', 'media'],
            'playback': ['playback', 'playbackmanager'],
            'gimbal': ['gimbal', 'djigimbal'],
            'flightcontroller': ['flightcontroller', 'flight'],
            'battery': ['battery', 'djibattery'],
            'remotecontroller': ['remotecontroller', 'remote'],
            'airlink': ['airlink', 'djiairlink'],
            'handheld': ['handheld', 'djihandheld'],
            'payload': ['payload', 'djipayload'],
            'rtk': ['rtk', 'djirtk'],
            'simulator': ['simulator', 'djisimulator'],
            'utils': ['util', 'common', 'error'],
            'mission': ['mission', 'waypoint', 'hotpoint'],
            'general': []
        }
        
        for category, keywords in categories.items():
            for keyword in keywords:
                if keyword in url_lower or keyword in text_lower:
                    return category
        
        return 'general'
    
    def extract_index_links(self):
        """Extraer todos los links del index principal"""
        print(f"🔍 Extrayendo links de: {self.index_url}")
        
        try:
            response = self.session.get(self.index_url, timeout=15)
            response.raise_for_status()
            
            soup = BeautifulSoup(response.content, 'html.parser')
            
            # Buscar todos los links
            all_links = []
            categorized_links = {}
            
            for link_tag in soup.find_all('a', href=True):
                href = link_tag['href']
                text = link_tag.get_text(strip=True)
                
                # Convertir a URL absoluta
                full_url = urljoin(self.index_url, href)
                
                if self.is_valid_dji_link(full_url):
                    category = self.categorize_link(full_url, text)
                    
                    link_info = {
                        'url': full_url,
                        'text': text,
                        'category': category,
                        'found_in': 'index'
                    }
                    
                    all_links.append(link_info)
                    
                    if category not in categorized_links:
                        categorized_links[category] = []
                    categorized_links[category].append(link_info)
                    
                    print(f"  📎 [{category}] {text[:50]}: {full_url}")
            
            # Guardar resultados
            self.save_results(all_links, categorized_links)
            
            return all_links, categorized_links
            
        except Exception as e:
            print(f"❌ Error extrayendo links: {e}")
            return [], {}
    
    def save_results(self, all_links, categorized_links):
        """Guardar resultados en archivos"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        # 1. Archivo JSON completo
        results = {
            'timestamp': datetime.now().isoformat(),
            'source_url': self.index_url,
            'total_links': len(all_links),
            'all_links': all_links,
            'categorized': categorized_links,
            'categories_count': {cat: len(links) for cat, links in categorized_links.items()}
        }
        
        with open(f'dji_index_links_{timestamp}.json', 'w', encoding='utf-8') as f:
            json.dump(results, f, indent=2, ensure_ascii=False)
        
        # 2. Archivo de texto legible
        with open(f'dji_index_links_{timestamp}.txt', 'w', encoding='utf-8') as f:
            f.write("📋 LINKS EXTRAÍDOS DEL INDEX PRINCIPAL DJI SDK\n")
            f.write("=" * 60 + "\n\n")
            f.write(f"🕐 Timestamp: {datetime.now().isoformat()}\n")
            f.write(f"🔗 URL Source: {self.index_url}\n")
            f.write(f"📊 Total Links: {len(all_links)}\n\n")
            
            # Resumen por categorías
            f.write("📊 RESUMEN POR CATEGORÍAS:\n")
            f.write("-" * 40 + "\n")
            for category, links in categorized_links.items():
                f.write(f"  {category.upper()}: {len(links)} links\n")
            f.write("\n")
            
            # Detalles por categoría
            for category, links in categorized_links.items():
                f.write(f"\n📁 CATEGORÍA: {category.upper()}\n")
                f.write("-" * 40 + "\n")
                for link in links:
                    f.write(f"  📎 {link['text']}\n")
                    f.write(f"     {link['url']}\n\n")
        
        # 3. Lista simple de URLs para el segundo script
        with open('dji_urls_for_crawling.txt', 'w', encoding='utf-8') as f:
            for link in all_links:
                f.write(f"{link['url']}\n")
        
        # 4. URLs por categoría
        for category, links in categorized_links.items():
            if links:  # Solo si hay links en esa categoría
                with open(f'dji_urls_{category}.txt', 'w', encoding='utf-8') as f:
                    for link in links:
                        f.write(f"{link['url']}\n")
        
        print(f"\n💾 Resultados guardados:")
        print(f"   📄 dji_index_links_{timestamp}.json - Datos completos")
        print(f"   📄 dji_index_links_{timestamp}.txt - Texto legible")
        print(f"   📄 dji_urls_for_crawling.txt - URLs para crawler")
        print(f"   📄 dji_urls_[categoria].txt - URLs por categoría")
    
    def show_summary(self, categorized_links):
        """Mostrar resumen en consola"""
        print(f"\n📊 RESUMEN DE EXTRACCIÓN:")
        print("=" * 50)
        
        total = sum(len(links) for links in categorized_links.values())
        print(f"📈 Total links encontrados: {total}")
        
        print(f"\n📁 Por categorías:")
        for category, links in sorted(categorized_links.items()):
            if links:
                print(f"   {category.upper()}: {len(links)} links")
                # Mostrar algunos ejemplos
                for i, link in enumerate(links[:3]):
                    print(f"      • {link['text'][:40]}...")
                if len(links) > 3:
                    print(f"      ... y {len(links) - 3} más")
                print()

def main():
    print("🚀 EXTRACTOR DE LINKS DEL INDEX DJI SDK")
    print("=" * 50)
    
    extractor = DJIIndexExtractor()
    all_links, categorized_links = extractor.extract_index_links()
    
    if all_links:
        extractor.show_summary(categorized_links)
        print("\n✅ Extracción completada!")
        print("📝 Ahora puedes usar los archivos generados para el crawler profundo.")
    else:
        print("❌ No se pudieron extraer links")

if __name__ == "__main__":
    main()
