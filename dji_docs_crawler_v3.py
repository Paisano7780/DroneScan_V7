#!/usr/bin/env python3
"""
DJI Documentation Crawler V3 - Con archivos de texto completos
Explora toda la documentación DJI SDK y guarda información detallada en texto
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

class DJIDocsCrawlerV3:
    def __init__(self, base_url="https://developer.dji.com/api-reference/android-api/"):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        })
        
        # Archivos de estado y datos
        self.progress_file = "dji_crawl_progress.json"
        self.data_file = "dji_docs_data.json"
        self.checkpoint_file = "dji_crawl_checkpoint.pkl"
        
        # NUEVOS: Archivos de texto para información completa
        self.full_content_file = "dji_docs_full_content.txt"
        self.media_manager_file = "dji_media_manager_info.txt"
        self.playback_manager_file = "dji_playback_manager_info.txt"
        self.camera_methods_file = "dji_camera_methods.txt"
        self.all_methods_file = "dji_all_methods_summary.txt"
        
        # Estado del crawler
        self.visited_urls = set()
        self.pending_urls = []
        self.all_docs = {}
        self.current_batch = 0
        self.max_batch_size = 15  # Reducir un poco para mejor calidad
        
        # Contadores por tipo de contenido
        self.media_manager_pages = []
        self.playback_manager_pages = []
        self.camera_pages = []
        self.all_methods = []
        
        # Cargar estado previo si existe
        self.load_progress()
        
        # Inicializar archivos de texto
        self.init_text_files()
    
    def init_text_files(self):
        """Inicializar archivos de texto con headers"""
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        
        # Archivo principal de contenido completo
        with open(self.full_content_file, 'w', encoding='utf-8') as f:
            f.write(f"""
===============================================================================
DJI SDK v4 - DOCUMENTACIÓN COMPLETA EXTRAÍDA
===============================================================================
Generado: {timestamp}
Crawler: DJI Docs Crawler V3
URL Base: {self.base_url}
===============================================================================

""")
        
        # Archivo específico de MediaManager
        with open(self.media_manager_file, 'w', encoding='utf-8') as f:
            f.write(f"""
===============================================================================
DJI SDK v4 - MEDIA MANAGER - DOCUMENTACIÓN COMPLETA
===============================================================================
Generado: {timestamp}
Descripción: Toda la información relacionada con DJI MediaManager
===============================================================================

""")
        
        # Archivo específico de PlaybackManager
        with open(self.playback_manager_file, 'w', encoding='utf-8') as f:
            f.write(f"""
===============================================================================
DJI SDK v4 - PLAYBACK MANAGER - DOCUMENTACIÓN COMPLETA
===============================================================================
Generado: {timestamp}
Descripción: Toda la información relacionada con DJI PlaybackManager
===============================================================================

""")
        
        # Archivo de métodos de cámara
        with open(self.camera_methods_file, 'w', encoding='utf-8') as f:
            f.write(f"""
===============================================================================
DJI SDK v4 - CAMERA METHODS - TODOS LOS MÉTODOS
===============================================================================
Generado: {timestamp}
Descripción: Todos los métodos disponibles para Camera
===============================================================================

""")
        
        # Resumen de todos los métodos
        with open(self.all_methods_file, 'w', encoding='utf-8') as f:
            f.write(f"""
===============================================================================
DJI SDK v4 - RESUMEN DE TODOS LOS MÉTODOS Y FUNCIONES
===============================================================================
Generado: {timestamp}
Descripción: Índice completo de métodos, funciones y callbacks
===============================================================================

""")
    
    def append_to_file(self, filename, content):
        """Agregar contenido a un archivo de texto"""
        try:
            with open(filename, 'a', encoding='utf-8') as f:
                f.write(content)
                f.write("\n")
        except Exception as e:
            print(f"❌ Error escribiendo a {filename}: {e}")
    
    def save_progress(self):
        """Guardar progreso actual"""
        progress = {
            'visited_urls': list(self.visited_urls),
            'pending_urls': self.pending_urls,
            'current_batch': self.current_batch,
            'timestamp': datetime.now().isoformat(),
            'total_processed': len(self.visited_urls),
            'total_pending': len(self.pending_urls),
            'media_manager_count': len(self.media_manager_pages),
            'playback_manager_count': len(self.playback_manager_pages),
            'camera_count': len(self.camera_pages),
            'total_methods': len(self.all_methods)
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
                'current_batch': self.current_batch,
                'media_manager_pages': self.media_manager_pages,
                'playback_manager_pages': self.playback_manager_pages,
                'camera_pages': self.camera_pages,
                'all_methods': self.all_methods
            }, f)
        
        print(f"💾 Progreso guardado: {len(self.visited_urls)} procesadas, {len(self.pending_urls)} pendientes")
        print(f"   📱 MediaManager: {len(self.media_manager_pages)} páginas")
        print(f"   🎮 PlaybackManager: {len(self.playback_manager_pages)} páginas")
        print(f"   📷 Camera: {len(self.camera_pages)} páginas")
        print(f"   ⚙️ Métodos totales: {len(self.all_methods)}")
    
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
                    self.media_manager_pages = data.get('media_manager_pages', [])
                    self.playback_manager_pages = data.get('playback_manager_pages', [])
                    self.camera_pages = data.get('camera_pages', [])
                    self.all_methods = data.get('all_methods', [])
                
                print(f"🔄 Recuperando progreso: {len(self.visited_urls)} ya procesadas")
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
    
    def extract_methods_from_content(self, soup, url):
        """Extraer métodos específicos del contenido"""
        methods = []
        
        # Buscar métodos de diferentes maneras
        method_selectors = [
            'div[id*="method"]',
            'div[class*="method"]',
            'section[id*="method"]',
            'div[id*="function"]',
            'h3[id*="method"]',
            'h4[id*="method"]',
            'code:contains("(")',
            'pre:contains("(")'
        ]
        
        for selector in method_selectors:
            try:
                elements = soup.select(selector)
                for elem in elements:
                    text = elem.get_text(strip=True)
                    if '(' in text and len(text) < 200:  # Probablemente un método
                        methods.append(text)
            except:
                continue
        
        # Buscar también en el texto general
        text_content = soup.get_text()
        lines = text_content.split('\n')
        for line in lines:
            line = line.strip()
            if ('(' in line and ')' in line and 
                any(keyword in line.lower() for keyword in ['public', 'void', 'boolean', 'string', 'int', 'callback']) and
                len(line) < 150):
                methods.append(line)
        
        return list(set(methods))  # Eliminar duplicados
    
    def extract_content_and_links(self, url):
        """Extraer contenido y links de una página"""
        try:
            print(f"🔍 Explorando: {url}")
            
            response = self.session.get(url, timeout=20)
            response.raise_for_status()
            
            soup = BeautifulSoup(response.content, 'html.parser')
            
            # Extraer contenido básico
            title = soup.title.string if soup.title else 'Sin título'
            
            # Extraer contenido completo de la página
            full_text = soup.get_text(separator='\n', strip=True)
            
            # Detectar tipo de contenido
            url_lower = url.lower()
            component_type = 'General'
            
            if 'mediamanager' in url_lower:
                component_type = 'MediaManager'
                print(f"📱 Encontrado contenido MediaManager")
                self.media_manager_pages.append(url)
                
                # Guardar información específica de MediaManager
                media_content = f"""
-------------------------------------------------------------------------------
URL: {url}
TÍTULO: {title}
FECHA: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
TIPO: MediaManager
-------------------------------------------------------------------------------

{full_text}

===============================================================================
"""
                self.append_to_file(self.media_manager_file, media_content)
                
            elif 'playback' in url_lower:
                component_type = 'PlaybackManager'
                print(f"🎮 Encontrado contenido PlaybackManager")
                self.playback_manager_pages.append(url)
                
                # Guardar información específica de PlaybackManager
                playback_content = f"""
-------------------------------------------------------------------------------
URL: {url}
TÍTULO: {title}
FECHA: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
TIPO: PlaybackManager
-------------------------------------------------------------------------------

{full_text}

===============================================================================
"""
                self.append_to_file(self.playback_manager_file, playback_content)
                
            elif 'camera' in url_lower:
                component_type = 'Camera'
                print(f"📷 Encontrado contenido Camera")
                self.camera_pages.append(url)
                
                # Guardar información específica de Camera
                camera_content = f"""
-------------------------------------------------------------------------------
URL: {url}
TÍTULO: {title}
FECHA: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
TIPO: Camera
-------------------------------------------------------------------------------

{full_text}

===============================================================================
"""
                self.append_to_file(self.camera_methods_file, camera_content)
            
            # Extraer métodos de esta página
            page_methods = self.extract_methods_from_content(soup, url)
            if page_methods:
                print(f"⚙️ Encontrados {len(page_methods)} métodos en esta página")
                self.all_methods.extend(page_methods)
                
                # Agregar métodos al resumen
                methods_content = f"""
-------------------------------------------------------------------------------
MÉTODOS ENCONTRADOS EN: {url}
TÍTULO: {title}
FECHA: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
TOTAL MÉTODOS: {len(page_methods)}
-------------------------------------------------------------------------------

"""
                for i, method in enumerate(page_methods, 1):
                    methods_content += f"{i:3d}. {method}\n"
                
                methods_content += "\n===============================================================================\n"
                self.append_to_file(self.all_methods_file, methods_content)
            
            # Guardar contenido completo en archivo principal
            full_content = f"""
===============================================================================
URL: {url}
TÍTULO: {title}
TIPO: {component_type}
FECHA: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
MÉTODOS ENCONTRADOS: {len(page_methods)}
===============================================================================

{full_text}

"""
            self.append_to_file(self.full_content_file, full_content)
            
            # Preparar estructura de datos
            content = {
                'url': url,
                'title': title,
                'component_type': component_type,
                'timestamp': datetime.now().isoformat(),
                'methods_count': len(page_methods),
                'content_length': len(full_text),
                'methods': page_methods[:20]  # Solo primeros 20 para JSON
            }
            
            # Extraer todos los links válidos
            new_links = []
            for link in soup.find_all('a', href=True):
                full_url = urljoin(url, link['href'])
                if self.is_valid_dji_url(full_url) and full_url not in self.visited_urls:
                    new_links.append(full_url)
            
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
                
                # Pausa para no sobrecargar el servidor
                time.sleep(1.0)  # Aumentar un poco la pausa
            
            # Guardar progreso cada 3 URLs (más frecuente)
            if processed_count % 3 == 0:
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
            print(f"⏸️ Pausa de 3 segundos entre lotes...")
            time.sleep(3)
        
        print("\n🎉 Crawling completado!")
        self.generate_final_summary()
    
    def generate_final_summary(self):
        """Generar resumen final de todo lo encontrado"""
        print(f"\n📋 RESUMEN FINAL DEL CRAWLING:")
        print(f"   📄 Total páginas procesadas: {len(self.visited_urls)}")
        print(f"   📱 MediaManager páginas: {len(self.media_manager_pages)}")
        print(f"   🎮 PlaybackManager páginas: {len(self.playback_manager_pages)}")
        print(f"   📷 Camera páginas: {len(self.camera_pages)}")
        print(f"   ⚙️ Total métodos encontrados: {len(self.all_methods)}")
        
        # Crear resumen final en archivo de texto
        summary_content = f"""
===============================================================================
RESUMEN FINAL - DJI SDK v4 CRAWLING COMPLETO
===============================================================================
Fecha de completado: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
Total páginas procesadas: {len(self.visited_urls)}
Total métodos encontrados: {len(self.all_methods)}

DISTRIBUCIÓN POR COMPONENTES:
- MediaManager páginas: {len(self.media_manager_pages)}
- PlaybackManager páginas: {len(self.playback_manager_pages)}
- Camera páginas: {len(self.camera_pages)}
- Otras páginas: {len(self.visited_urls) - len(self.media_manager_pages) - len(self.playback_manager_pages) - len(self.camera_pages)}

ARCHIVOS GENERADOS:
1. {self.full_content_file} - Contenido completo de todas las páginas
2. {self.media_manager_file} - Información específica de MediaManager
3. {self.playback_manager_file} - Información específica de PlaybackManager
4. {self.camera_methods_file} - Métodos y funciones de Camera
5. {self.all_methods_file} - Resumen de todos los métodos encontrados

===============================================================================
"""
        
        with open('dji_crawl_RESUMEN_FINAL.txt', 'w', encoding='utf-8') as f:
            f.write(summary_content)
        
        print(f"   💾 Resumen final guardado en: dji_crawl_RESUMEN_FINAL.txt")
        print(f"\n📁 ARCHIVOS CREADOS:")
        print(f"   📜 {self.full_content_file}")
        print(f"   📱 {self.media_manager_file}")
        print(f"   🎮 {self.playback_manager_file}")
        print(f"   📷 {self.camera_methods_file}")
        print(f"   ⚙️ {self.all_methods_file}")
        print(f"   📋 dji_crawl_RESUMEN_FINAL.txt")

def main():
    crawler = DJIDocsCrawlerV3()
    
    if len(sys.argv) > 1:
        command = sys.argv[1].lower()
        
        if command == 'continue':
            print("🔄 Continuando crawl desde checkpoint...")
            crawler.start_crawl()
        elif command == 'restart':
            print("🔄 Reiniciando crawl desde cero...")
            # Limpiar archivos de progreso
            files_to_clean = [
                crawler.progress_file, crawler.data_file, crawler.checkpoint_file,
                crawler.full_content_file, crawler.media_manager_file,
                crawler.playback_manager_file, crawler.camera_methods_file,
                crawler.all_methods_file, 'dji_crawl_RESUMEN_FINAL.txt'
            ]
            for file in files_to_clean:
                if os.path.exists(file):
                    os.remove(file)
            crawler = DJIDocsCrawlerV3()
            crawler.start_crawl("https://developer.dji.com/api-reference/android-api/index.html")
        elif command == 'batch':
            batch_size = int(sys.argv[2]) if len(sys.argv) > 2 else 10
            print(f"🔄 Procesando lote de {batch_size} URLs...")
            crawler.process_batch(batch_size)
        elif command == 'summary':
            crawler.generate_final_summary()
        else:
            print("❌ Comando no reconocido")
            print("Uso: python dji_docs_crawler_v3.py [continue|restart|batch|summary]")
    else:
        print("🚀 Iniciando crawl normal...")
        crawler.start_crawl("https://developer.dji.com/api-reference/android-api/index.html")

if __name__ == "__main__":
    main()
