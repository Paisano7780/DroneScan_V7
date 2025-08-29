#!/usr/bin/env python3
"""
DJI Documentation Crawler V2 - Con checkpoint y recuperación
Explora toda la documentación DJI SDK de manera incremental y resiliente
"""

import requests
import time
import json
import os
import sys
from urllib.parse import urljoin, urlparse
from bs4 import BeautifulSoup
from datetime import datetime
import pickle

class DJIDocsCrawler:
    def __init__(self, base_url="https://developer.dji.com/api-reference/android-api/"):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        })
        
        # Archivos de estado
        self.progress_file = "dji_crawl_progress.json"
        self.data_file = "dji_docs_data.json"
        self.checkpoint_file = "dji_crawl_checkpoint.pkl"
        
        # Estado del crawler
        self.visited_urls = set()
        self.pending_urls = []
        self.all_docs = {}
        self.current_batch = 0
        self.max_batch_size = 20  # Procesar de a 20 URLs
        
        # Cargar estado previo si existe
        self.load_progress()
    
    def save_progress(self):
        """Guardar progreso actual"""
        progress = {
            'visited_urls': list(self.visited_urls),
            'pending_urls': self.pending_urls,
            'current_batch': self.current_batch,
            'timestamp': datetime.now().isoformat(),
            'total_processed': len(self.visited_urls),
            'total_pending': len(self.pending_urls)
        }
        
        # Guardar progreso en JSON
        with open(self.progress_file, 'w', encoding='utf-8') as f:
            json.dump(progress, f, indent=2, ensure_ascii=False)
        
        # Guardar datos en JSON
        with open(self.data_file, 'w', encoding='utf-8') as f:
            json.dump(self.all_docs, f, indent=2, ensure_ascii=False)
        
        # Guardar checkpoint completo
        with open(self.checkpoint_file, 'wb') as f:
            pickle.dump({
                'visited_urls': self.visited_urls,
                'pending_urls': self.pending_urls,
                'all_docs': self.all_docs,
                'current_batch': self.current_batch
            }, f)
        
        print(f"💾 Progreso guardado: {len(self.visited_urls)} procesadas, {len(self.pending_urls)} pendientes")
    
    def load_progress(self):
        """Cargar progreso previo si existe"""
        try:
            if os.path.exists(self.checkpoint_file):
                with open(self.checkpoint_file, 'rb') as f:
                    data = pickle.load(f)
                    self.visited_urls = data.get('visited_urls', set())
                    self.pending_urls = data.get('pending_urls', [])
                    self.all_docs = data.get('all_docs', {})
                    self.current_batch = data.get('current_batch', 0)
                
                print(f"🔄 Recuperando progreso: {len(self.visited_urls)} ya procesadas, {len(self.pending_urls)} pendientes")
                return True
            
        except Exception as e:
            print(f"⚠️ Error cargando progreso: {e}")
        
        return False
    
    def is_valid_dji_url(self, url):
        """Verificar si la URL es válida para DJI docs"""
        if not url:
            return False
        
        parsed = urlparse(url)
        if parsed.netloc != 'developer.dji.com':
            return False
        
        if not parsed.path.startswith('/api-reference/android-api/'):
            return False
        
        # Filtros adicionales
        skip_patterns = [
            'javascript:', 'mailto:', '#', 'tel:',
            '.pdf', '.zip', '.jpg', '.png', '.gif'
        ]
        
        for pattern in skip_patterns:
            if pattern in url.lower():
                return False
        
        return True
    
    def extract_content_and_links(self, url):
        """Extraer contenido y links de una página"""
        try:
            print(f"🔍 Explorando: {url}")
            
            response = self.session.get(url, timeout=15)
            response.raise_for_status()
            
            soup = BeautifulSoup(response.content, 'html.parser')
            
            # Extraer contenido relevante
            content = {
                'url': url,
                'title': soup.title.string if soup.title else 'Sin título',
                'timestamp': datetime.now().isoformat()
            }
            
            # Buscar contenido específico de DJI SDK
            sections = {}
            
            # Métodos y funciones
            methods = soup.find_all(['div', 'section'], class_=lambda x: x and any(
                term in str(x).lower() for term in ['method', 'function', 'api']
            ))
            if methods:
                sections['methods'] = [m.get_text(strip=True)[:500] for m in methods[:10]]
            
            # Descripción general
            description_tags = soup.find_all(['p', 'div'], class_=lambda x: x and 'description' in str(x).lower())
            if description_tags:
                sections['description'] = [d.get_text(strip=True)[:300] for d in description_tags[:5]]
            
            # Headers importantes
            headers = soup.find_all(['h1', 'h2', 'h3', 'h4'])
            if headers:
                sections['headers'] = [h.get_text(strip=True) for h in headers[:15]]
            
            # Código de ejemplo
            code_blocks = soup.find_all(['code', 'pre'])
            if code_blocks:
                sections['code_examples'] = [c.get_text(strip=True)[:200] for c in code_blocks[:5]]
            
            content['sections'] = sections
            
            # Extraer todos los links válidos
            new_links = []
            for link in soup.find_all('a', href=True):
                full_url = urljoin(url, link['href'])
                if self.is_valid_dji_url(full_url) and full_url not in self.visited_urls:
                    new_links.append(full_url)
            
            # Detectar tipo de contenido
            url_lower = url.lower()
            if 'mediamanager' in url_lower:
                content['component_type'] = 'MediaManager'
                print(f"📱 Encontrado contenido MediaManager")
            elif 'playback' in url_lower:
                content['component_type'] = 'PlaybackManager'
                print(f"🎮 Encontrado contenido PlaybackManager")
            elif 'camera' in url_lower:
                content['component_type'] = 'Camera'
                print(f"📷 Encontrado contenido Camera")
            
            return content, new_links
            
        except requests.exceptions.Timeout:
            print(f"⏱️ Timeout en: {url}")
            return None, []
        except requests.exceptions.RequestException as e:
            print(f"❌ Error de red en {url}: {e}")
            return None, []
        except Exception as e:
            print(f"❌ Error procesando {url}: {e}")
            return None, []
    
    def process_batch(self, batch_size=None):
        """Procesar un lote de URLs"""
        if batch_size is None:
            batch_size = self.max_batch_size
        
        if not self.pending_urls:
            print("✅ No hay URLs pendientes para procesar")
            return False
        
        # Tomar el próximo lote
        batch = self.pending_urls[:batch_size]
        self.pending_urls = self.pending_urls[batch_size:]
        
        print(f"\n🚀 Procesando lote {self.current_batch + 1}: {len(batch)} URLs")
        
        processed_count = 0
        for url in batch:
            if url in self.visited_urls:
                continue
            
            content, new_links = self.extract_content_and_links(url)
            
            if content:
                self.all_docs[url] = content
                self.visited_urls.add(url)
                processed_count += 1
                
                # Agregar nuevos links únicos
                for link in new_links:
                    if link not in self.visited_urls and link not in self.pending_urls:
                        self.pending_urls.append(link)
                
                print(f"  ✅ Procesado: {content.get('title', 'Sin título')[:50]}")
                
                # Pequeña pausa para no sobrecargar el servidor
                time.sleep(0.5)
            
            # Guardar progreso cada 5 URLs
            if processed_count % 5 == 0:
                self.save_progress()
        
        self.current_batch += 1
        self.save_progress()
        
        print(f"📊 Lote completado: {processed_count} nuevas páginas procesadas")
        print(f"📈 Total acumulado: {len(self.visited_urls)} páginas, {len(self.pending_urls)} pendientes")
        
        return len(self.pending_urls) > 0
    
    def start_crawl(self, start_url=None):
        """Iniciar o continuar el crawling"""
        if start_url and not self.pending_urls and not self.visited_urls:
            self.pending_urls.append(start_url)
            print(f"🚀 Iniciando crawl desde: {start_url}")
        elif self.pending_urls:
            print(f"🔄 Continuando crawl con {len(self.pending_urls)} URLs pendientes")
        else:
            print("❌ No hay URLs para procesar")
            return
        
        # Procesar en lotes
        while self.pending_urls:
            has_more = self.process_batch()
            
            if not has_more:
                break
            
            # Pausa entre lotes
            print(f"⏸️ Pausa de 2 segundos entre lotes...")
            time.sleep(2)
        
        print("\n🎉 Crawling completado!")
        self.generate_summary()
    
    def generate_summary(self):
        """Generar resumen de lo encontrado"""
        print(f"\n📋 RESUMEN DEL CRAWLING:")
        print(f"   📄 Total páginas procesadas: {len(self.visited_urls)}")
        
        # Agrupar por tipo de componente
        components = {}
        for url, data in self.all_docs.items():
            comp_type = data.get('component_type', 'General')
            if comp_type not in components:
                components[comp_type] = []
            components[comp_type].append(url)
        
        print(f"   🧩 Componentes encontrados:")
        for comp_type, urls in components.items():
            print(f"      {comp_type}: {len(urls)} páginas")
        
        # Guardar resumen
        summary = {
            'total_pages': len(self.visited_urls),
            'components': {k: len(v) for k, v in components.items()},
            'completion_time': datetime.now().isoformat(),
            'component_details': components
        }
        
        with open('dji_crawl_summary.json', 'w', encoding='utf-8') as f:
            json.dump(summary, f, indent=2, ensure_ascii=False)
        
        print(f"   💾 Resumen guardado en: dji_crawl_summary.json")

def main():
    crawler = DJIDocsCrawler()
    
    if len(sys.argv) > 1:
        command = sys.argv[1].lower()
        
        if command == 'continue':
            print("🔄 Continuando crawl desde checkpoint...")
            crawler.start_crawl()
        elif command == 'restart':
            print("🔄 Reiniciando crawl desde cero...")
            # Limpiar archivos de progreso
            for file in [crawler.progress_file, crawler.data_file, crawler.checkpoint_file]:
                if os.path.exists(file):
                    os.remove(file)
            crawler = DJIDocsCrawler()
            crawler.start_crawl("https://developer.dji.com/api-reference/android-api/index.html")
        elif command == 'batch':
            batch_size = int(sys.argv[2]) if len(sys.argv) > 2 else 10
            print(f"🔄 Procesando lote de {batch_size} URLs...")
            crawler.process_batch(batch_size)
        elif command == 'summary':
            crawler.generate_summary()
        else:
            print("❌ Comando no reconocido")
            print("Uso: python dji_docs_crawler_v2.py [continue|restart|batch|summary]")
    else:
        print("🚀 Iniciando crawl normal...")
        crawler.start_crawl("https://developer.dji.com/api-reference/android-api/index.html")

if __name__ == "__main__":
    main()
