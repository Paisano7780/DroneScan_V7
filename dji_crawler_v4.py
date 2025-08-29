#!/usr/bin/env python3
"""
DJI Documentation Crawler V4 - Con URLs Conocidas
Crawl específico para URLs conocidas de DJI SDK con máximo detalle
"""

import requests
import time
import json
import os
import re
from urllib.parse import urljoin, urlparse
from bs4 import BeautifulSoup
from datetime import datetime

class DJISpecificCrawler:
    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        })
        
        # Archivos de salida
        self.full_content_file = "dji_docs_COMPLETO.txt"
        self.media_manager_file = "dji_MEDIA_MANAGER_detallado.txt"
        self.playback_manager_file = "dji_PLAYBACK_MANAGER_detallado.txt"
        self.camera_methods_file = "dji_CAMERA_METHODS_completo.txt"
        self.all_methods_file = "dji_TODOS_LOS_METODOS.txt"
        self.final_summary_file = "dji_RESUMEN_FINAL_COMPLETO.txt"
        
        # Datos recolectados
        self.all_content = []
        self.methods_found = []
        self.media_manager_content = []
        self.playback_manager_content = []
        self.camera_content = []
        
        # Cargar URLs conocidas
        self.load_known_urls()
    
    def load_known_urls(self):
        """Cargar URLs desde archivo"""
        try:
            with open('dji_known_urls.txt', 'r', encoding='utf-8') as f:
                self.urls = [line.strip() for line in f if line.strip() and not line.startswith('#')]
            print(f"📂 Cargadas {len(self.urls)} URLs conocidas")
        except FileNotFoundError:
            print("❌ Archivo dji_known_urls.txt no encontrado. Ejecuta primero dji_known_urls.py")
            self.urls = []
    
    def extract_methods(self, soup, url):
        """Extraer todos los métodos de una página"""
        methods = []
        
        # Buscar diferentes patrones de métodos
        method_patterns = [
            r'(\w+)\s*\([^)]*\)',  # pattern básico: method(params)
            r'public\s+\w+\s+(\w+)\s*\([^)]*\)',  # public type method(params)
            r'static\s+\w+\s+(\w+)\s*\([^)]*\)',  # static type method(params)
            r'void\s+(\w+)\s*\([^)]*\)',  # void method(params)
            r'(\w+)\s*:\s*\([^)]*\)\s*->'  # kotlin style
        ]
        
        text_content = soup.get_text()
        
        for pattern in method_patterns:
            matches = re.findall(pattern, text_content, re.MULTILINE | re.IGNORECASE)
            for match in matches:
                if len(match) > 2 and not match.startswith('_'):  # Filtrar métodos válidos
                    methods.append(match)
        
        # Buscar también en elementos específicos
        for elem in soup.find_all(['code', 'pre', 'span'], class_=lambda x: x and any(
            term in str(x).lower() for term in ['method', 'function', 'api', 'code']
        )):
            text = elem.get_text(strip=True)
            # Buscar patrones de método en estos elementos
            method_matches = re.findall(r'(\w+)\s*\([^)]*\)', text)
            methods.extend([m for m in method_matches if len(m) > 2])
        
        return list(set(methods))  # Eliminar duplicados
    
    def categorize_content(self, url, content, methods):
        """Categorizar contenido según el tipo"""
        url_lower = url.lower()
        
        entry = {
            'url': url,
            'content': content,
            'methods': methods,
            'timestamp': datetime.now().isoformat()
        }
        
        if 'mediamanager' in url_lower:
            self.media_manager_content.append(entry)
            print(f"📱 MediaManager: {len(methods)} métodos encontrados")
        elif 'playback' in url_lower:
            self.playback_manager_content.append(entry)
            print(f"🎮 PlaybackManager: {len(methods)} métodos encontrados")
        elif 'camera' in url_lower:
            self.camera_content.append(entry)
            print(f"📷 Camera: {len(methods)} métodos encontrados")
        
        self.methods_found.extend(methods)
    
    def process_single_url(self, url):
        """Procesar una URL específica"""
        try:
            print(f"\n🔍 Procesando: {url}")
            
            response = self.session.get(url, timeout=20)
            response.raise_for_status()
            
            soup = BeautifulSoup(response.content, 'html.parser')
            
            # Extraer título
            title = soup.title.string if soup.title else 'Sin título'
            
            # Extraer todo el contenido de texto
            full_text = soup.get_text()
            
            # Limpiar el texto
            lines = [line.strip() for line in full_text.split('\n') if line.strip()]
            clean_text = '\n'.join(lines)
            
            # Extraer métodos
            methods = self.extract_methods(soup, url)
            
            # Crear entrada completa
            content_entry = {
                'url': url,
                'title': title,
                'full_text': clean_text,
                'methods': methods,
                'method_count': len(methods),
                'character_count': len(clean_text),
                'timestamp': datetime.now().isoformat()
            }
            
            self.all_content.append(content_entry)
            self.categorize_content(url, content_entry, methods)
            
            print(f"  ✅ Título: {title}")
            print(f"  📊 Métodos: {len(methods)}")
            print(f"  📄 Caracteres: {len(clean_text)}")
            
            return True
            
        except Exception as e:
            print(f"  ❌ Error: {e}")
            return False
    
    def save_all_content(self):
        """Guardar todo el contenido en archivos"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        # 1. Archivo con TODO el contenido
        with open(self.full_content_file, 'w', encoding='utf-8') as f:
            f.write("📚 DOCUMENTACIÓN COMPLETA DJI SDK v4 ANDROID API\n")
            f.write("=" * 80 + "\n\n")
            f.write(f"🕐 Generado: {datetime.now().isoformat()}\n")
            f.write(f"📊 Total páginas: {len(self.all_content)}\n")
            f.write(f"🔧 Total métodos: {len(set(self.methods_found))}\n\n")
            
            for i, entry in enumerate(self.all_content, 1):
                f.write(f"\n{'='*80}\n")
                f.write(f"PÁGINA {i}: {entry['title']}\n")
                f.write(f"URL: {entry['url']}\n")
                f.write(f"Métodos encontrados: {entry['method_count']}\n")
                f.write(f"{'='*80}\n\n")
                
                f.write("📝 CONTENIDO COMPLETO:\n")
                f.write("-" * 40 + "\n")
                f.write(entry['full_text'])
                f.write("\n\n")
                
                if entry['methods']:
                    f.write("🔧 MÉTODOS ENCONTRADOS:\n")
                    f.write("-" * 40 + "\n")
                    for method in entry['methods']:
                        f.write(f"  • {method}\n")
                    f.write("\n")
        
        # 2. Archivo específico de MediaManager
        if self.media_manager_content:
            with open(self.media_manager_file, 'w', encoding='utf-8') as f:
                f.write("📱 MEDIA MANAGER - INFORMACIÓN DETALLADA\n")
                f.write("=" * 60 + "\n\n")
                
                for entry in self.media_manager_content:
                    f.write(f"🔗 URL: {entry['url']}\n")
                    f.write(f"📊 Métodos: {len(entry['methods'])}\n")
                    f.write("-" * 40 + "\n")
                    f.write(entry['content']['full_text'])
                    f.write("\n\n")
                    if entry['methods']:
                        f.write("🔧 MÉTODOS:\n")
                        for method in entry['methods']:
                            f.write(f"  • {method}\n")
                    f.write("\n" + "="*60 + "\n\n")
        
        # 3. Archivo específico de PlaybackManager
        if self.playback_manager_content:
            with open(self.playback_manager_file, 'w', encoding='utf-8') as f:
                f.write("🎮 PLAYBACK MANAGER - INFORMACIÓN DETALLADA\n")
                f.write("=" * 60 + "\n\n")
                
                for entry in self.playback_manager_content:
                    f.write(f"🔗 URL: {entry['url']}\n")
                    f.write(f"📊 Métodos: {len(entry['methods'])}\n")
                    f.write("-" * 40 + "\n")
                    f.write(entry['content']['full_text'])
                    f.write("\n\n")
                    if entry['methods']:
                        f.write("🔧 MÉTODOS:\n")
                        for method in entry['methods']:
                            f.write(f"  • {method}\n")
                    f.write("\n" + "="*60 + "\n\n")
        
        # 4. Todos los métodos encontrados
        unique_methods = sorted(set(self.methods_found))
        with open(self.all_methods_file, 'w', encoding='utf-8') as f:
            f.write("🔧 TODOS LOS MÉTODOS ENCONTRADOS EN DJI SDK\n")
            f.write("=" * 60 + "\n\n")
            f.write(f"📊 Total métodos únicos: {len(unique_methods)}\n\n")
            
            for i, method in enumerate(unique_methods, 1):
                f.write(f"{i:3d}. {method}\n")
        
        # 5. Resumen final
        with open(self.final_summary_file, 'w', encoding='utf-8') as f:
            f.write("📋 RESUMEN FINAL - CRAWLING DJI SDK\n")
            f.write("=" * 50 + "\n\n")
            f.write(f"🕐 Completado: {datetime.now().isoformat()}\n")
            f.write(f"📄 Páginas procesadas: {len(self.all_content)}\n")
            f.write(f"🔧 Métodos únicos: {len(unique_methods)}\n")
            f.write(f"📱 Páginas MediaManager: {len(self.media_manager_content)}\n")
            f.write(f"🎮 Páginas PlaybackManager: {len(self.playback_manager_content)}\n")
            f.write(f"📷 Páginas Camera: {len(self.camera_content)}\n\n")
            
            f.write("📂 ARCHIVOS GENERADOS:\n")
            f.write(f"  • {self.full_content_file} - Contenido completo\n")
            f.write(f"  • {self.media_manager_file} - MediaManager detallado\n")
            f.write(f"  • {self.playback_manager_file} - PlaybackManager detallado\n")
            f.write(f"  • {self.all_methods_file} - Todos los métodos\n")
            f.write(f"  • {self.final_summary_file} - Este resumen\n\n")
            
            # Estadísticas por página
            f.write("📊 ESTADÍSTICAS POR PÁGINA:\n")
            f.write("-" * 40 + "\n")
            for entry in self.all_content:
                f.write(f"  {entry['title'][:40]:<40} | {entry['method_count']:3d} métodos\n")
        
        print(f"\n💾 ARCHIVOS GUARDADOS:")
        print(f"  📄 {self.full_content_file}")
        print(f"  📱 {self.media_manager_file}")
        print(f"  🎮 {self.playback_manager_file}")
        print(f"  🔧 {self.all_methods_file}")
        print(f"  📋 {self.final_summary_file}")
    
    def crawl_all(self):
        """Crawlear todas las URLs conocidas"""
        if not self.urls:
            print("❌ No hay URLs para procesar")
            return
        
        print(f"🚀 Iniciando crawling de {len(self.urls)} URLs conocidas")
        print("=" * 60)
        
        success_count = 0
        
        for i, url in enumerate(self.urls, 1):
            print(f"\n[{i}/{len(self.urls)}]", end=" ")
            
            if self.process_single_url(url):
                success_count += 1
            
            # Pausa entre requests
            time.sleep(1)
            
            # Checkpoint cada 5 URLs
            if i % 5 == 0:
                print(f"  💾 Checkpoint: {success_count}/{i} exitosas")
        
        print(f"\n🎉 Crawling completado!")
        print(f"✅ Exitosas: {success_count}/{len(self.urls)}")
        
        self.save_all_content()

def main():
    print("🎯 DJI DOCUMENTATION CRAWLER V4")
    print("=" * 50)
    
    crawler = DJISpecificCrawler()
    crawler.crawl_all()
    
    print("\n✅ Proceso completado! Revisa los archivos generados.")

if __name__ == "__main__":
    main()
