#!/usr/bin/env python3
"""
Crawler Específico para DJI MediaManager
Extrae información detallada de la página específica de MediaManager
"""

import requests
import re
import json
from urllib.parse import urljoin, urlparse
from bs4 import BeautifulSoup
from datetime import datetime

class MediaManagerCrawler:
    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
            'Accept-Language': 'en-US,en;q=0.5',
            'Accept-Encoding': 'gzip, deflate, br',
            'Connection': 'keep-alive',
        })
        
        self.url = "https://developer.dji.com/api-reference/android-api/Components/Camera/DJIMediaManager.html"
        
    def extract_detailed_content(self):
        """Extraer contenido detallado de MediaManager"""
        try:
            print(f"🔍 Analizando MediaManager: {self.url}")
            
            response = self.session.get(self.url, timeout=20)
            response.raise_for_status()
            
            soup = BeautifulSoup(response.content, 'html.parser')
            
            # Extraer título
            title = soup.title.string if soup.title else 'Sin título'
            print(f"📄 Título: {title}")
            
            # Extraer todo el contenido
            full_text = soup.get_text()
            
            # Buscar métodos de diferentes maneras
            methods_found = self.extract_methods_comprehensive(soup, full_text)
            
            # Buscar información específica de MediaManager
            media_manager_info = self.extract_media_manager_specifics(soup, full_text)
            
            # Buscar ejemplos de código
            code_examples = self.extract_code_examples(soup)
            
            # Buscar callbacks y interfaces
            callbacks = self.extract_callbacks(soup, full_text)
            
            # Crear reporte completo
            report = {
                'url': self.url,
                'title': title,
                'timestamp': datetime.now().isoformat(),
                'methods_found': methods_found,
                'media_manager_info': media_manager_info,
                'code_examples': code_examples,
                'callbacks': callbacks,
                'full_text': full_text,
                'character_count': len(full_text)
            }
            
            self.save_detailed_report(report)
            self.analyze_findings(report)
            
            return report
            
        except Exception as e:
            print(f"❌ Error analizando MediaManager: {e}")
            return None
    
    def extract_methods_comprehensive(self, soup, text):
        """Extraer métodos de manera comprehensiva"""
        methods = set()
        
        # Patrón 1: Métodos Java típicos
        java_patterns = [
            r'public\s+\w+\s+(\w+)\s*\([^)]*\)',
            r'private\s+\w+\s+(\w+)\s*\([^)]*\)',
            r'protected\s+\w+\s+(\w+)\s*\([^)]*\)',
            r'static\s+\w+\s+(\w+)\s*\([^)]*\)',
            r'void\s+(\w+)\s*\([^)]*\)',
            r'boolean\s+(\w+)\s*\([^)]*\)',
            r'int\s+(\w+)\s*\([^)]*\)',
            r'String\s+(\w+)\s*\([^)]*\)',
        ]
        
        for pattern in java_patterns:
            matches = re.findall(pattern, text, re.IGNORECASE | re.MULTILINE)
            methods.update(matches)
        
        # Patrón 2: Métodos DJI específicos
        dji_patterns = [
            r'(\w*[Dd]ownload\w*)\s*\(',
            r'(\w*[Ff]etch\w*)\s*\(',
            r'(\w*[Gg]et\w*)\s*\(',
            r'(\w*[Ss]et\w*)\s*\(',
            r'(\w*[Rr]efresh\w*)\s*\(',
            r'(\w*[Ll]ist\w*)\s*\(',
            r'(\w*[Ff]ile\w*)\s*\(',
            r'(\w*[Mm]edia\w*)\s*\(',
            r'(\w*[Cc]allback\w*)\s*\(',
        ]
        
        for pattern in dji_patterns:
            matches = re.findall(pattern, text, re.IGNORECASE)
            methods.update(matches)
        
        # Patrón 3: Buscar en elementos de código específicos
        code_elements = soup.find_all(['code', 'pre', 'span'], class_=lambda x: x and any(
            term in str(x).lower() for term in ['method', 'function', 'api', 'code', 'highlight']
        ))
        
        for elem in code_elements:
            elem_text = elem.get_text()
            # Buscar métodos en estos elementos
            method_matches = re.findall(r'(\w+)\s*\([^)]*\)', elem_text)
            methods.update([m for m in method_matches if len(m) > 2 and not m.startswith('_')])
        
        # Filtrar métodos válidos
        valid_methods = [m for m in methods if len(m) > 2 and m.isalpha()]
        
        return sorted(list(set(valid_methods)))
    
    def extract_media_manager_specifics(self, soup, text):
        """Extraer información específica de MediaManager"""
        info = {}
        
        # Buscar secciones específicas de MediaManager
        sections = soup.find_all(['div', 'section', 'article'], class_=lambda x: x and any(
            term in str(x).lower() for term in ['content', 'method', 'description', 'api']
        ))
        
        media_manager_content = []
        for section in sections:
            section_text = section.get_text(strip=True)
            if 'mediamanager' in section_text.lower() or 'media manager' in section_text.lower():
                media_manager_content.append(section_text[:1000])  # Primeros 1000 chars
        
        info['specific_sections'] = media_manager_content
        
        # Buscar menciones de download/fetch
        download_mentions = re.findall(r'[^.]*download[^.]*\.', text, re.IGNORECASE)
        info['download_mentions'] = download_mentions[:10]  # Primeras 10 menciones
        
        fetch_mentions = re.findall(r'[^.]*fetch[^.]*\.', text, re.IGNORECASE)
        info['fetch_mentions'] = fetch_mentions[:10]
        
        # Buscar storage/SD card mentions
        storage_mentions = re.findall(r'[^.]*(?:storage|sdcard|sd card)[^.]*\.', text, re.IGNORECASE)
        info['storage_mentions'] = storage_mentions[:10]
        
        return info
    
    def extract_code_examples(self, soup):
        """Extraer ejemplos de código"""
        code_blocks = soup.find_all(['pre', 'code'])
        examples = []
        
        for block in code_blocks:
            code_text = block.get_text(strip=True)
            if len(code_text) > 20:  # Solo bloques de código significativos
                examples.append({
                    'code': code_text[:500],  # Primeros 500 caracteres
                    'tag': block.name,
                    'class': block.get('class', [])
                })
        
        return examples
    
    def extract_callbacks(self, soup, text):
        """Extraer información sobre callbacks"""
        callback_patterns = [
            r'(\w*[Cc]allback\w*)',
            r'(\w*[Ll]istener\w*)',
            r'interface\s+(\w+)',
            r'(\w*CompletionCallback\w*)',
            r'(\w*DownloadListener\w*)',
        ]
        
        callbacks = set()
        for pattern in callback_patterns:
            matches = re.findall(pattern, text, re.IGNORECASE)
            callbacks.update(matches)
        
        return sorted(list(callbacks))
    
    def save_detailed_report(self, report):
        """Guardar reporte detallado"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        # Guardar JSON completo
        with open(f'mediamanager_analysis_{timestamp}.json', 'w', encoding='utf-8') as f:
            json.dump(report, f, indent=2, ensure_ascii=False)
        
        # Guardar reporte legible
        with open(f'mediamanager_analysis_{timestamp}.txt', 'w', encoding='utf-8') as f:
            f.write("📱 ANÁLISIS DETALLADO DE DJI MEDIAMANAGER\n")
            f.write("=" * 70 + "\n\n")
            f.write(f"🔗 URL: {report['url']}\n")
            f.write(f"📄 Título: {report['title']}\n")
            f.write(f"🕐 Timestamp: {report['timestamp']}\n")
            f.write(f"📊 Caracteres totales: {report['character_count']}\n\n")
            
            # Métodos encontrados
            f.write("🔧 MÉTODOS ENCONTRADOS:\n")
            f.write("-" * 40 + "\n")
            for i, method in enumerate(report['methods_found'], 1):
                f.write(f"{i:3d}. {method}\n")
            f.write(f"\nTotal métodos: {len(report['methods_found'])}\n\n")
            
            # Información específica de MediaManager
            f.write("📱 INFORMACIÓN ESPECÍFICA DE MEDIAMANAGER:\n")
            f.write("-" * 40 + "\n")
            
            if report['media_manager_info']['download_mentions']:
                f.write("📥 MENCIONES DE DOWNLOAD:\n")
                for mention in report['media_manager_info']['download_mentions']:
                    f.write(f"  • {mention.strip()}\n")
                f.write("\n")
            
            if report['media_manager_info']['fetch_mentions']:
                f.write("🔄 MENCIONES DE FETCH:\n")
                for mention in report['media_manager_info']['fetch_mentions']:
                    f.write(f"  • {mention.strip()}\n")
                f.write("\n")
            
            if report['media_manager_info']['storage_mentions']:
                f.write("💾 MENCIONES DE STORAGE/SD:\n")
                for mention in report['media_manager_info']['storage_mentions']:
                    f.write(f"  • {mention.strip()}\n")
                f.write("\n")
            
            # Callbacks
            f.write("🔄 CALLBACKS Y LISTENERS:\n")
            f.write("-" * 40 + "\n")
            for callback in report['callbacks']:
                f.write(f"  • {callback}\n")
            f.write("\n")
            
            # Ejemplos de código
            if report['code_examples']:
                f.write("💻 EJEMPLOS DE CÓDIGO:\n")
                f.write("-" * 40 + "\n")
                for i, example in enumerate(report['code_examples'][:5], 1):
                    f.write(f"Ejemplo {i}:\n")
                    f.write(f"{example['code']}\n")
                    f.write("-" * 20 + "\n")
                f.write("\n")
            
            # Contenido completo al final
            f.write("📄 CONTENIDO COMPLETO:\n")
            f.write("=" * 70 + "\n")
            f.write(report['full_text'])
        
        print(f"💾 Análisis guardado en:")
        print(f"  📄 mediamanager_analysis_{timestamp}.txt")
        print(f"  📄 mediamanager_analysis_{timestamp}.json")
    
    def analyze_findings(self, report):
        """Analizar hallazgos y dar conclusiones"""
        print(f"\n📊 ANÁLISIS DE HALLAZGOS:")
        print("=" * 50)
        
        methods = report['methods_found']
        print(f"🔧 Total métodos encontrados: {len(methods)}")
        
        # Buscar métodos de descarga específicos
        download_methods = [m for m in methods if 'download' in m.lower()]
        fetch_methods = [m for m in methods if 'fetch' in m.lower()]
        file_methods = [m for m in methods if 'file' in m.lower()]
        
        print(f"📥 Métodos de descarga: {len(download_methods)}")
        for method in download_methods:
            print(f"    • {method}")
        
        print(f"🔄 Métodos de fetch: {len(fetch_methods)}")
        for method in fetch_methods:
            print(f"    • {method}")
        
        print(f"📁 Métodos de archivo: {len(file_methods)}")
        for method in file_methods[:10]:  # Solo los primeros 10
            print(f"    • {method}")
        
        # Conclusiones
        print(f"\n🎯 CONCLUSIONES:")
        print("-" * 30)
        
        if download_methods:
            print("✅ MediaManager SÍ tiene métodos de descarga")
        else:
            print("❌ MediaManager NO tiene métodos de descarga evidentes")
        
        if any('playback' in m.lower() for m in methods):
            print("🎮 Se mencionan métodos relacionados con Playback")
        else:
            print("❌ No se mencionan métodos de Playback en MediaManager")

def main():
    print("🔍 ANÁLISIS ESPECÍFICO DE DJI MEDIAMANAGER")
    print("=" * 60)
    
    crawler = MediaManagerCrawler()
    result = crawler.extract_detailed_content()
    
    if result:
        print("\n✅ Análisis completado! Revisa los archivos generados.")
    else:
        print("\n❌ Error en el análisis")

if __name__ == "__main__":
    main()
