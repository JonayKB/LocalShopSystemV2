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
        imageSrc: '/assets/news1.jpg',
        title: 'Nuevas plantas en stock',
        description: 'Hemos recibido una nueva variedad de plantas exóticas. Ven y descubre las últimas incorporaciones a nuestro catálogo.'
      },
      {
        imageSrc: '/assets/news2.jpg',
        title: 'Descuentos especiales',
        description: 'Aprovecha nuestros descuentos exclusivos por tiempo limitado en todas las categorías. ¡No te lo pierdas!'
      },
      {
        imageSrc: '/assets/news3.jpg',
        title: 'Eventos y talleres',
        description: 'Únete a nuestros talleres de jardinería y aprende de los expertos. Consulta nuestro calendario de eventos para más detalles.'
      }
    ])
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
      }}
    >
      <Link
        to={"admin/Home"}
        style={{
          background: '#4caf50',
          color: 'white',
          padding: '14px 32px',
          borderRadius: '8px',
          textDecoration: 'none',
          fontWeight: 'bold',
          fontSize: '20px',
          boxShadow: '0 4px 12px rgba(0,0,0,0.2)',
          transition: 'background 0.2s',
          position: 'absolute',
          top: '20px',
          right: '20px',
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
          width: '64px',
          height: '64px',
          borderRadius: '50%',
          boxShadow: '0 2px 8px rgba(0,0,0,0.2)',
          objectFit: 'contain',
        }}
      />
      <h1 style={{ marginBottom: '24px', textShadow: '2px 2px 8px #000' }}>
        Kiosco Botanico
      </h1>
      <div style={{ width: '100%', height: '400px', overflow: 'hidden', borderRadius: '12px', boxShadow: '0 4px 12px rgba(0,0,0,0.3)', marginBottom: '48px', position: 'relative' }}>
        {carouselImages.map((src, index) => (
          <img
            key={index}
            src={src}
            alt={`Carousel ${index + 1}`}
            style={{
              width: '100%',
              height: '100%',
              objectFit: 'cover',
              position: 'absolute',
              top: 0,
              left: `${index * 100}%`,
              transition: 'left 0.5s ease-in-out',
              animation: `slide ${carouselImages.length * 5}s infinite`
            }}
          />
        ))}
        <style>
          {`
            @keyframes slide {
              0% { left: 0%; }
              ${100 / carouselImages.length}% { left: -100%; }
              ${200 / carouselImages.length}% { left: -200%; }
              ${300 / carouselImages.length}% { left: -300%; }
              ${400 / carouselImages.length}% { left: -400%; }
            }
          `}
        </style>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '40px', marginTop: '48px', width: '100%', maxWidth: '80%' }}>
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