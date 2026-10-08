import React from 'react'
import { Link } from 'react-router-dom'
import {
  motion,
  useReducedMotion,
  useScroll,
  useSpring,
  Variants,
} from 'framer-motion'
import {
  ArrowDown,
  BookOpen,
  Box,
  Candy,
  Coffee,
  Flame,
  Gem,
  IceCreamCone,
  Lock,
  LucideIcon,
  Magnet,
  Mail,
  MapPin,
  Mountain,
  Palette,
  Palmtree,
  Printer,
  ShoppingBag,
  Sparkles,
  Sprout,
  Store,
} from 'lucide-react'
import '../styles/home.css'

type Props = {}

type Line = {
  text: string;
  highlight?: boolean; // texto destacado (p. ej. ¡NOVEDAD!)
  sub?: boolean;       // sub-elemento (sangrado)
  mail?: string;       // si existe, se muestra como enlace mailto
}

type Section = {
  icon: LucideIcon;
  title: string;
  tag?: string;
  lines: Line[];
  note?: string;
}

// Nombre del negocio
const BUSINESS_NAME = 'Kiosco Botanico'

// Marca de la tienda de Nieves: cambia aquí el nombre cuando lo tengas definido.
const NIEVES_STORE_NAME = 'La tienda de Maca'

const SECTIONS: Section[] = [
  {
    icon: Sparkles,
    title: 'Artículos Novedad',
    tag: 'Novedad',
    lines: [{ text: 'Cromos Liga Reposición' }],
  },
  {
    icon: Sprout,
    title: 'Plantas y semillas',
    lines: [{ text: 'Novedades en plantas disponibles en macetas M11, M12 y M13' }],
  },
  {
    icon: Candy,
    title: 'Momentos dulces',
    lines: [
      { text: 'Disfruta de tus chocolatinas de siempre' },
      { text: '¡NOVEDAD! SNIKERS CREAMY', highlight: true },
    ],
  },
  {
    icon: IceCreamCone,
    title: 'Helados',
    tag: 'Producto ecológico',
    lines: [{ text: 'Helados Kalise y Palettas' }],
    note: 'Palettas, ¡los helados más buscados del planeta!',
  },
  {
    icon: Coffee,
    title: 'Pausa para el café',
    lines: [
      { text: 'Disponemos de café para llevar a elegir entre: café solo, cortado, descafeinado, capuccino y chocolate' },
      { text: '¡Todo de la marca Nescafé, con la calidad de siempre!' },
    ],
  },
  {
    icon: Mail,
    title: 'Souvenirs',
    lines: [
      { text: 'Amplia gama de postales de distintos tamaños y modelos' },
      { text: 'Postal arena, postal estándar, postal cuadrada, postal apaisada' },
    ],
  },
  {
    icon: Magnet,
    title: 'Imanes',
    lines: [
      { text: 'Gran muestrario de imanes relieve con acabados brillantes' },
      { text: 'Novedades en imanes con imágenes de las islas y el Puerto de la Cruz. Acabado brillante/mate. Un detalle espectacular como recuerdo.' },
    ],
  },
  {
    icon: ShoppingBag,
    title: 'El Rincón de POPO',
    lines: [{ text: 'Totebag, sombreros, gorras, toallas, tazas, abanicos, posavasos, etc.' }],
  },
  {
    icon: Palmtree,
    title: 'COCOS',
    lines: [{ text: '¡Al mejor precio!' }],
  },
  {
    icon: Palette,
    title: 'Artículos de newys',
    lines: [
      { text: 'El arte en tu ropa' },
      { text: 'Gorras, camisas, totebag, tazas… un sinfín de artículos con imágenes únicas. El arte canario plasmado en artículos de recuerdo.' },
    ],
  },
  {
    icon: Gem,
    title: 'Milena',
    lines: [{ text: 'Amplia gama de pulseras y tobilleras' }],
  },
  {
    icon: Mountain,
    title: 'Pulseras Volcánicas',
    lines: [
      { text: 'Pulseras volcánicas, chakra, zodiacos, olivina…' },
      { text: 'Amplio surtido de pulseras de Canarias, elige la tuya' },
    ],
  },
  {
    icon: Store,
    title: 'Pequeño Bazar',
    lines: [{ text: 'Disponemos de artículos de necesidad. Pregunta por lo que necesites' }],
  },
  {
    icon: Printer,
    title: 'Fotocopias',
    lines: [
      { text: 'Servicio de impresión en A4 y A3 Blanco y Negro y COLOR.' },
      { text: 'Plastificados y encuadernaciones' },
      { text: 'Imprime tus BONO para regalo aquí' },
      { text: 'kbotanico@gmail.com', mail: 'kbotanico@gmail.com', sub: true },
    ],
  },
  {
    icon: Box,
    title: 'Impresión 3D',
    lines: [
      { text: 'Servicio de impresión 3D, distintos tamaños y colores. Alta calidad, productos de confianza.' },
      { text: 'Haz tus encargos personalizados.' },
    ],
  },
  {
    icon: BookOpen,
    title: 'Libros de Canarias',
    lines: [
      { text: 'Surtido de libros de Canarias, desde guías de Tenerife hasta literatura, gastronomía, flora y fauna (algunos bajo pedido)' },
      { text: 'Calendarios de pared y sobremesa' },
    ],
  },
  {
    icon: Flame,
    title: NIEVES_STORE_NAME,
    tag: 'Artesanal',
    lines: [
      { text: 'Jabones artesanales, waxmell, velas aromáticas…' },
      { text: 'Deleita tus sentidos con los olores de los productos artesanales de la trastienda de Maca' },
      { text: `Disponible en ${BUSINESS_NAME}` },
    ],
  },
]

const EASE: [number, number, number, number] = [0.22, 1, 0.36, 1]

/* ---------- Variantes de animación ---------- */
const heroVariants: Variants = {
  hidden: {},
  show: { transition: { staggerChildren: 0.11, delayChildren: 0.1 } },
}

const heroItem: Variants = {
  hidden: { opacity: 0, y: 18 },
  show: { opacity: 1, y: 0, transition: { duration: 0.7, ease: EASE } },
}

const rowVariants: Variants = {
  hidden: {},
  show: { transition: { staggerChildren: 0.08 } },
}

const rowItem: Variants = {
  hidden: { opacity: 0, y: 16 },
  show: { opacity: 1, y: 0, transition: { duration: 0.6, ease: EASE } },
}

const lineDraw: Variants = {
  hidden: { scaleX: 0 },
  show: { scaleX: 1, transition: { duration: 0.9, ease: EASE } },
}

/* ---------- Ilustración vectorial: hoja botánica dibujada en línea ---------- */
const LEAF_PATHS = [
  'M200 50 C305 105 335 225 200 345 C65 225 95 105 200 50 Z', // contorno
  'M200 50 L200 425',                                         // nervio central y tallo
  'M200 120 C172 128 152 146 138 168',                        // nervios izquierda
  'M200 170 C168 180 144 202 130 230',
  'M200 225 C172 234 150 254 142 282',
  'M200 120 C228 128 248 146 262 168',                        // nervios derecha
  'M200 170 C232 180 256 202 270 230',
  'M200 225 C228 234 250 254 258 282',
]

const BotanicalMark = ({ reduced }: { reduced: boolean }) => (
  <svg viewBox="0 0 400 440" fill="none" role="img" aria-label="Ilustración de una hoja">
    <motion.circle
      cx="200"
      cy="215"
      r="190"
      stroke="var(--border-strong)"
      strokeWidth="1"
      initial={reduced ? false : { pathLength: 0, opacity: 0 }}
      animate={{ pathLength: 1, opacity: 1 }}
      transition={{ duration: 1.8, ease: EASE }}
    />
    <motion.g
      style={{ transformOrigin: '200px 425px' }}
      animate={reduced ? undefined : { rotate: [-1.4, 1.4, -1.4] }}
      transition={{ duration: 9, repeat: Infinity, ease: 'easeInOut' }}
    >
      {/* Hoja principal */}
      {LEAF_PATHS.map((d, i) => (
        <motion.path
          key={`a-${i}`}
          d={d}
          stroke="currentColor"
          strokeWidth="1.6"
          strokeLinecap="round"
          strokeLinejoin="round"
          initial={reduced ? false : { pathLength: 0 }}
          animate={{ pathLength: 1 }}
          transition={{ duration: 1.5, delay: 0.35 + i * 0.12, ease: EASE }}
        />
      ))}
      {/* Hoja secundaria, más pequeña */}
      <g transform="translate(200 395) rotate(42) scale(0.42) translate(-200 -345)" opacity="0.7">
        {LEAF_PATHS.map((d, i) => (
          <motion.path
            key={`b-${i}`}
            d={d}
            stroke="currentColor"
            strokeWidth="1.6"
            strokeLinecap="round"
            strokeLinejoin="round"
            vectorEffect="non-scaling-stroke"
            initial={reduced ? false : { pathLength: 0 }}
            animate={{ pathLength: 1 }}
            transition={{ duration: 1.3, delay: 1.1 + i * 0.1, ease: EASE }}
          />
        ))}
      </g>
    </motion.g>
  </svg>
)

const HomeScreen = (props: Props) => {
  const reduced = !!useReducedMotion()

  // Barra de progreso de lectura (fina y sólida)
  const { scrollYProgress } = useScroll()
  const progress = useSpring(scrollYProgress, { stiffness: 140, damping: 30, restDelta: 0.001 })

  // Con "reducir movimiento" los elementos se muestran directamente
  const hidden = reduced ? false : 'hidden'

  return (
    <div className="home theme-light">
      <motion.div className="home-progress" style={{ scaleX: progress }} />

      <motion.header
        className="home-header"
        initial={reduced ? false : { opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.6 }}
      >
        <div className="home-wrap">
          <div className="home-brand">
            <img src="/favicon.ico" alt={`Logo ${BUSINESS_NAME}`} />
            <span>{BUSINESS_NAME}</span>
          </div>
          <Link to="admin/Home" className="home-admin-link">
            <Lock size={15} strokeWidth={2} aria-hidden="true" />
            Administración
          </Link>
        </div>
      </motion.header>

      <div className="home-wrap">
        <section className="home-hero">
          <motion.div variants={heroVariants} initial={hidden} animate="show">

            <motion.h1 className="home-title" variants={heroItem}>
              {BUSINESS_NAME}
            </motion.h1>

            <motion.p className="home-subtitle" variants={heroItem}>
              Fotocopias, plantas, regalos y recuerdos de Canarias. Descubre todas las novedades de nuestras secciones.
            </motion.p>

            <motion.span className="home-meta" variants={heroItem}>
              <MapPin size={17} strokeWidth={1.9} aria-hidden="true" />
              Puerto de la Cruz, Tenerife · Enfrente del Jardín Botánico
            </motion.span>

            <motion.div className="home-actions" variants={heroItem}>
              <a href="#secciones" className="home-btn">
                Ver novedades
                <ArrowDown size={17} strokeWidth={2} aria-hidden="true" />
              </a>
            </motion.div>
          </motion.div>

          <div className="home-art">
            <BotanicalMark reduced={reduced} />
          </div>
        </section>

        <motion.div
          id="secciones"
          className="home-section-head"
          initial={hidden}
          whileInView="show"
          viewport={{ once: true, amount: 0.6 }}
          variants={rowItem}
        >
          <h2>Novedades y secciones</h2>
          <small>{SECTIONS.length} secciones</small>
        </motion.div>

        <ol className="home-list">
          {SECTIONS.map((section, index) => {
            const Icon = section.icon
            return (
              <motion.li
                key={section.title}
                className="home-row"
                variants={rowVariants}
                initial={hidden}
                whileInView="show"
                viewport={{ once: true, amount: 0.25 }}
              >
                <motion.span className="home-row-line" variants={lineDraw} aria-hidden="true" />

                <motion.span className="home-row-index" variants={rowItem}>
                  {String(index + 1).padStart(2, '0')}
                </motion.span>

                <motion.div className="home-row-title" variants={rowItem}>
                  <span className="home-row-icon" aria-hidden="true">
                    <Icon size={22} strokeWidth={1.7} />
                  </span>
                  <div>
                    <h3>{section.title}</h3>
                    {section.tag && <span className="home-tag">{section.tag}</span>}
                  </div>
                </motion.div>

                <motion.div className="home-row-body" variants={rowItem}>
                  {section.lines.map((line, i) => (
                    <p
                      key={i}
                      className={['home-line', line.highlight ? 'is-new' : '', line.sub ? 'is-sub' : '']
                        .join(' ')
                        .trim()}
                    >
                      {line.mail ? (
                        <a href={`mailto:${line.mail}`}>
                          <Mail size={15} strokeWidth={1.9} aria-hidden="true" />
                          {line.text}
                        </a>
                      ) : (
                        line.text
                      )}
                    </p>
                  ))}
                  {section.note && <p className="home-note">{section.note}</p>}
                </motion.div>
              </motion.li>
            )
          })}
        </ol>
      </div>

      <footer className="home-footer">
        <div className="home-wrap">
          <span>
            © {new Date().getFullYear()} <strong>{BUSINESS_NAME}</strong>
          </span>
          <span>Puerto de la Cruz, Tenerife</span>
        </div>
      </footer>
    </div>
  )
}

export default HomeScreen
