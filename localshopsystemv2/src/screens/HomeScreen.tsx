import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import InfoDisplay from '../components/InfoDisplay'

type Props = {}

type NewItem = {
  imageSrc: string;
  title: string;
  description: string;
}

const HomeScreen = (props: Props) => {
  const [news, setnews] = useState<NewItem[]>([])
  const [carouselImages, setcarouselImages] = useState<string[]>([])

  useEffect(() => {
    setnews([
      {
        imageSrc: '/assets/fotocopias.png',
        title: 'Fotocopias',
        description: 'Realizamos fotocopias en blanco y negro y a color, en varios tamaños y acabados. Calidad garantizada y precios competitivos.'
      },
      {
        imageSrc: '/assets/hotwheels.jpg',
        title: 'Hotwheels',
        description: 'Explora nuestra nueva colección de Hotwheels para coleccionistas. Modelos de ultimas ediciones, al momento.'
      },
      {
        imageSrc: '/assets/plantas.jpg',
        title: 'Plantas de temporada',
        description: 'Visita nuestra sección de plantas y semillas de temporada y descubre las variedades más frescas y hermosas para tu hogar.'
      },

      {
        imageSrc: '/assets/golosinas.jpg',
        title: 'Golosinas',
        description: 'Descubre nuestra nueva colección de golosinas, perfectas para disfrutar en cualquier momento.'
      },
      {
        imageSrc: '/assets/helados.jpg',
        title: 'Helados',
        description: 'Helados de diversos sabores, perfectos para refrescarte en cualquier momento.'
      },
      {
        imageSrc: '/assets/prensa.jpg',
        title: 'Prensa',
        description: 'Periódicos y revistas nacionales e internacionales, para mantenerte informado.'
      },
      {
        imageSrc: '/assets/bebidas.jpg',
        title: 'Bebidas',
        description: 'Refrescos, jugos y bebidas energéticas para mantenerte hidratado.'
      },
    ])
    setcarouselImages([])
  }, [])

  return (
    <div
      style={{
        flex: 1,
        backgroundColor: '#2a2d3a',
        minHeight: '100vh',
        color: 'white',
        padding: '40px',
        fontSize: '22px',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        width: '98%',
        boxSizing: 'border-box',
        fontFamily: `'Segoe UI', 'Roboto', 'Helvetica Neue', sans-serif`,
        position: 'relative',
        gap: '20px',
        overflowX: 'hidden',
        scrollBehavior: 'smooth',

      }}
    >
      <Link
        to={"admin/Home"}
        style={{
          background: '#4caf50',
          color: 'white',
          padding: '1vw 2vw',
          borderRadius: '8px',
          textDecoration: 'none',
          fontWeight: 'bold',
          boxShadow: '0 4px 12px rgba(0,0,0,0.2)',
          transition: 'background 0.2s',
          position: 'absolute',
          top: '20px',
          right: '20px',
          fontSize: 'calc(12px + 1vw)'
        }}
        onMouseOver={e => (e.currentTarget.style.background = '#388e3c')}
        onMouseOut={e => (e.currentTarget.style.background = '#4caf50')}
      >
        Administración
      </Link>
      <img
        src="/favicon.ico"
        alt="Logo"
        style={{
          position: 'absolute',
          top: '20px',
          left: '20px',
          borderRadius: '50%',
          boxShadow: '0 2px 8px rgba(0,0,0,0.2)',
          objectFit: 'contain',
          width: '10vw',
          aspectRatio: '1 / 1',
          maxWidth: '150px',
        }}
      />
      <h1 style={{ marginBottom: '24px', textShadow: '2px 2px 8px #000', fontSize: 'calc(24px + 2vw)', fontWeight: '700' }}>
        Kiosco Botanico
      </h1>
      
      <div style={{ display: 'flex', flexDirection: 'column', gap: '40px', marginTop: '48px', width: '100%' }}>
        {news.map((item: any, index) => (
          <>
            <InfoDisplay
              key={index}
              imageSrc={item.imageSrc}
              title={item.title}
              description={item.description}
              orientation={index % 2 === 0 ? 'image-left' : 'image-right'}
            />
          </>
        ))}

      </div>
    </div>
  )
}

export default HomeScreen