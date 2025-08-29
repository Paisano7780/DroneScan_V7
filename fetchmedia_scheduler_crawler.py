#!/usr/bin/env python3
"""
Crawler Específico para DJI FetchMediaTaskScheduler
Extrae información detallada del FetchMediaTaskScheduler y explora todos los links hacia abajo
"""

import requests
import re
import json
import time
from urllib.parse import urljoin, urlparse
from bs4 import BeautifulSoup
from datetime import datetime

class FetchMediaTaskSchedulerCrawler:
    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
            'Accept-Language': 'en-US,en;q=0.5',
            'Accept-Encoding': 'gzip, deflate, br',
            'Connection': 'keep-alive',
        })
        
        self.base_url = "https://developer.dji.com/api-reference/android-api/Components/Camera/DJIMediaManager_FetchMediaTaskScheduler.html"
        self.visited_urls = set()
        self.all_content = []
        self.all_methods = []
        
    def is_valid_related_url(self, url):
        """Verificar si la URL es válida y relacionada"""
        if not url:
            return False
        
        parsed = urlparse(url)
        if parsed.netloc != 'developer.dji.com':
            return False
        
        if not parsed.path.startswith('/api-reference/android-api/'):
            return False
        
        # Filtrar URLs relacionadas con media, fetch, download
        relevant_terms = [
            'media', 'fetch', 'download', 'task', 'scheduler',
            'file', 'camera', 'callback', 'listener'
        ]
        
        url_lower = url.lower()
        if any(term in url_lower for term in relevant_terms):
            return True
        
        return False
    
    def extract_methods_and_content(self, url):
        """Extraer métodos y contenido detallado de una URL"""
        try:
            print(f"🔍 Analizando: {url}")
            
            response = self.session.get(url, timeout=20)
            response.raise_for_status()
            
            soup = BeautifulSoup(response.content, 'html.parser')
            
            # Extraer título
            title = soup.title.string if soup.title else 'Sin título'
            
            # Extraer contenido completo
            full_text = soup.get_text()
            
            # Buscar métodos específicos
            methods = self.extract_methods_comprehensive(soup, full_text)
            
            # Buscar información específica de downloads/fetch/tasks
            specific_info = self.extract_specific_info(soup, full_text, url)
            
            # Buscar ejemplos de código
            code_examples = self.extract_code_examples(soup)
            
            # Buscar callbacks y listeners
            callbacks = self.extract_callbacks(soup, full_text)
            
            # Buscar links relacionados para explorar
            related_links = self.extract_related_links(soup, url)
            
            # Crear entrada de contenido
            content_entry = {
                'url': url,
                'title': title,
                'methods': methods,
                'specific_info': specific_info,
                'code_examples': code_examples,
                'callbacks': callbacks,
                'related_links': related_links,
                'full_text': full_text[:5000],  # Primeros 5000 caracteres
                'character_count': len(full_text),
                'timestamp': datetime.now().isoformat()
            }
            
            self.all_content.append(content_entry)
            self.all_methods.extend(methods)
            
            print(f"  ✅ {title}")
            print(f"  🔧 Métodos: {len(methods)}")
            print(f"  🔗 Links relacionados: {len(related_links)}")
            
            return content_entry, related_links
            
        except Exception as e:
            print(f"  ❌ Error: {e}")
            return None, []
    
    def extract_methods_comprehensive(self, soup, text):
        """Extraer métodos de manera comprehensiva"""
        methods = set()
        
        # Patrones específicos para DJI SDK
        method_patterns = [
            r'(\w+)\s*\([^)]*\)\s*(?:throws\s+\w+)?',  # Métodos generales
            r'public\s+\w+\s+(\w+)\s*\([^)]*\)',       # Métodos públicos
            r'void\s+(\w+)\s*\([^)]*\)',               # Métodos void
            r'boolean\s+(\w+)\s*\([^)]*\)',            # Métodos boolean
            r'(\w*[Dd]ownload\w*)\s*\(',               # Métodos de download
            r'(\w*[Ff]etch\w*)\s*\(',                  # Métodos de fetch
            r'(\w*[Tt]ask\w*)\s*\(',                   # Métodos de task
            r'(\w*[Ss]chedule\w*)\s*\(',               # Métodos de schedule
            r'(\w*[Cc]allback\w*)\s*\(',               # Métodos de callback
            r'(\w*[Ll]istener\w*)\s*\(',               # Métodos de listener
        ]
        
        for pattern in method_patterns:
            matches = re.findall(pattern, text, re.IGNORECASE | re.MULTILINE)
            methods.update(matches)
        
        # Buscar en elementos de código específicos
        code_elements = soup.find_all(['code', 'pre', 'span'], class_=lambda x: x and any(
            term in str(x).lower() for term in ['method', 'function', 'api', 'code', 'highlight']
        ))
        
        for elem in code_elements:
            elem_text = elem.get_text()
            method_matches = re.findall(r'(\w+)\s*\([^)]*\)', elem_text)
            methods.update([m for m in method_matches if len(m) > 2])
        
        # Filtrar métodos válidos
        valid_methods = [m for m in methods if len(m) > 2 and m.isalpha() and not m.startswith('_')]
        
        return sorted(list(set(valid_methods)))
    
    def extract_specific_info(self, soup, text, url):
        """Extraer información específica sobre downloads, fetch, tasks"""
        info = {}
        
        # Buscar menciones específicas
        info['download_mentions'] = re.findall(r'[^.]*download[^.]*\.', text, re.IGNORECASE)[:15]
        info['fetch_mentions'] = re.findall(r'[^.]*fetch[^.]*\.', text, re.IGNORECASE)[:15]
        info['task_mentions'] = re.findall(r'[^.]*task[^.]*\.', text, re.IGNORECASE)[:15]
        info['scheduler_mentions'] = re.findall(r'[^.]*schedul[^.]*\.', text, re.IGNORECASE)[:10]
        
        # Buscar parámetros y tipos de retorno
        info['return_types'] = re.findall(r'(?:Return|Returns?):\s*([^\n]+)', text, re.IGNORECASE)
        info['parameters'] = re.findall(r'(?:Parameter|Input Parameters?):\s*([^\n]+)', text, re.IGNORECASE)
        
        # Buscar descripciones de métodos
        method_descriptions = []
        for section in soup.find_all(['div', 'section'], class_=lambda x: x and 'method' in str(x).lower()):
            desc_text = section.get_text(strip=True)
            if len(desc_text) > 50:
                method_descriptions.append(desc_text[:300])
        
        info['method_descriptions'] = method_descriptions[:10]
        
        return info
    
    def extract_code_examples(self, soup):
        """Extraer ejemplos de código"""
        code_blocks = soup.find_all(['pre', 'code'])
        examples = []
        
        for block in code_blocks:
            code_text = block.get_text(strip=True)
            if len(code_text) > 20:
                examples.append({
                    'code': code_text[:800],  # Primeros 800 caracteres
                    'tag': block.name,
                    'class': block.get('class', [])
                })
        
        return examples[:10]  # Solo los primeros 10
    
    def extract_callbacks(self, soup, text):
        """Extraer información sobre callbacks y listeners"""
        callback_patterns = [
            r'(\w*[Cc]allback\w*)',
            r'(\w*[Ll]istener\w*)',
            r'interface\s+(\w+)',
            r'(\w*CompletionCallback\w*)',
            r'(\w*DownloadListener\w*)',
            r'(\w*TaskCallback\w*)',
            r'(\w*ProgressCallback\w*)',
        ]
        
        callbacks = set()
        for pattern in callback_patterns:
            matches = re.findall(pattern, text, re.IGNORECASE)
            callbacks.update(matches)
        
        return sorted(list(callbacks))
    
    def extract_related_links(self, soup, base_url):
        """Extraer links relacionados para explorar"""
        links = []
        
        for link in soup.find_all('a', href=True):
            href = link['href']
            full_url = urljoin(base_url, href)
            
            if self.is_valid_related_url(full_url) and full_url not in self.visited_urls:
                link_text = link.get_text(strip=True)
                links.append({
                    'url': full_url,
                    'text': link_text,
                    'context': str(link.parent)[:200] if link.parent else ''
                })
        
        return links
    
    def crawl_deep(self, start_url, max_depth=3):
        """Crawlear en profundidad desde la URL inicial"""
        print(f"🚀 Iniciando crawling profundo desde: {start_url}")
        print(f"📊 Profundidad máxima: {max_depth}")
        print("=" * 80)
        
        urls_to_process = [(start_url, 0)]  # (url, depth)
        
        while urls_to_process:
            current_url, depth = urls_to_process.pop(0)
            
            if current_url in self.visited_urls or depth > max_depth:
                continue
            
            self.visited_urls.add(current_url)
            
            print(f"\n[Nivel {depth}] ", end="")
            content, related_links = self.extract_methods_and_content(current_url)
            
            if content and depth < max_depth:
                # Agregar links relacionados para el siguiente nivel
                for link_info in related_links:
                    if link_info['url'] not in self.visited_urls:
                        urls_to_process.append((link_info['url'], depth + 1))
            
            # Pausa entre requests
            time.sleep(1)
        
        print(f"\n🎉 Crawling completado!")
        print(f"📄 Total páginas analizadas: {len(self.all_content)}")
        print(f"🔧 Total métodos únicos: {len(set(self.all_methods))}")
    
    def save_comprehensive_report(self):
        """Guardar reporte comprehensivo"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        # Archivo de texto detallado
        with open(f'fetchmediataskscheduler_analysis_{timestamp}.txt', 'w', encoding='utf-8') as f:
            f.write("🎯 ANÁLISIS COMPREHENSIVO DE FETCHMEDIATASKSCHEDULER\n")
            f.write("=" * 80 + "\n\n")
            f.write(f"🕐 Completado: {datetime.now().isoformat()}\n")
            f.write(f"📄 Páginas analizadas: {len(self.all_content)}\n")
            f.write(f"🔧 Métodos únicos: {len(set(self.all_methods))}\n\n")
            
            # Métodos únicos encontrados
            unique_methods = sorted(set(self.all_methods))
            f.write("🔧 TODOS LOS MÉTODOS ENCONTRADOS:\n")
            f.write("-" * 50 + "\n")
            for i, method in enumerate(unique_methods, 1):
                f.write(f"{i:3d}. {method}\n")
            f.write(f"\nTotal: {len(unique_methods)} métodos\n\n")
            
            # Análisis por página
            f.write("📄 ANÁLISIS POR PÁGINA:\n")
            f.write("=" * 50 + "\n")
            
            for i, content in enumerate(self.all_content, 1):
                f.write(f"\n📑 PÁGINA {i}: {content['title']}\n")
                f.write(f"🔗 URL: {content['url']}\n")
                f.write(f"🔧 Métodos: {len(content['methods'])}\n")
                f.write(f"📊 Caracteres: {content['character_count']}\n")
                f.write("-" * 60 + "\n")
                
                # Métodos específicos de esta página
                if content['methods']:
                    f.write("🔧 MÉTODOS DE ESTA PÁGINA:\n")
                    for method in content['methods']:
                        f.write(f"  • {method}\n")
                    f.write("\n")
                
                # Información específica
                if content['specific_info']['download_mentions']:
                    f.write("📥 MENCIONES DE DOWNLOAD:\n")
                    for mention in content['specific_info']['download_mentions'][:5]:
                        f.write(f"  • {mention.strip()}\n")
                    f.write("\n")
                
                if content['specific_info']['fetch_mentions']:
                    f.write("🔄 MENCIONES DE FETCH:\n")
                    for mention in content['specific_info']['fetch_mentions'][:5]:
                        f.write(f"  • {mention.strip()}\n")
                    f.write("\n")
                
                if content['specific_info']['task_mentions']:
                    f.write("📋 MENCIONES DE TASK:\n")
                    for mention in content['specific_info']['task_mentions'][:5]:
                        f.write(f"  • {mention.strip()}\n")
                    f.write("\n")
                
                # Callbacks
                if content['callbacks']:
                    f.write("🔄 CALLBACKS/LISTENERS:\n")
                    for callback in content['callbacks'][:10]:
                        f.write(f"  • {callback}\n")
                    f.write("\n")
                
                # Ejemplos de código (los más relevantes)
                if content['code_examples']:
                    f.write("💻 EJEMPLOS DE CÓDIGO:\n")
                    for j, example in enumerate(content['code_examples'][:3], 1):
                        f.write(f"Código {j}:\n")
                        f.write(f"{example['code']}\n")
                        f.write("-" * 30 + "\n")
                    f.write("\n")
                
                # Contenido parcial
                f.write("📄 CONTENIDO (EXTRACTO):\n")
                f.write(f"{content['full_text']}\n")
                f.write("\n" + "="*80 + "\n")
        
        # Archivo JSON para datos estructurados
        with open(f'fetchmediataskscheduler_analysis_{timestamp}.json', 'w', encoding='utf-8') as f:
            json.dump({
                'timestamp': datetime.now().isoformat(),
                'summary': {
                    'total_pages': len(self.all_content),
                    'unique_methods': len(set(self.all_methods)),
                    'visited_urls': list(self.visited_urls)
                },
                'unique_methods': sorted(set(self.all_methods)),
                'all_content': self.all_content
            }, f, indent=2, ensure_ascii=False)
        
        print(f"\n💾 Reportes guardados:")
        print(f"  📄 fetchmediataskscheduler_analysis_{timestamp}.txt")
        print(f"  📄 fetchmediataskscheduler_analysis_{timestamp}.json")
        
        return timestamp

def main():
    print("🎯 CRAWLER ESPECÍFICO DE FETCHMEDIATASKSCHEDULER")
    print("=" * 80)
    
    crawler = FetchMediaTaskSchedulerCrawler()
    
    # Crawlear en profundidad
    crawler.crawl_deep(crawler.base_url, max_depth=2)
    
    # Guardar reporte comprehensivo
    timestamp = crawler.save_comprehensive_report()
    
    print(f"\n✅ Análisis completo terminado!")
    print(f"📊 Revisa los archivos generados para información detallada.")

if __name__ == "__main__":
    main()
